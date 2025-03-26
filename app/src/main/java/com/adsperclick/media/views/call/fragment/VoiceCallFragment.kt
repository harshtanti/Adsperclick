package com.adsperclick.media.views.call.fragment

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.adsperclick.media.R
import com.adsperclick.media.data.dataModels.CallParticipant
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentVoiceCallBinding
import com.adsperclick.media.services.VoiceCallService
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.DialogUtils
import com.adsperclick.media.utils.gone
import com.adsperclick.media.views.call.adapters.ParticipantAdapter
import com.adsperclick.media.views.call.viewmodel.CallViewModel
import com.adsperclick.media.views.homeActivity.SharedHomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class VoiceCallFragment : Fragment() {

    companion object {
        private const val TAG = "VoiceCallFragment"
        private const val PERMISSION_REQ_ID = 22
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }

    // You need to add your App ID here
    private val myAppId = Constants.AGORA_APP_ID
    private var channelName:String?=null
    private var token:String?=null
    private var myUid = 0

    private lateinit var binding: FragmentVoiceCallBinding
    private val callViewModel: CallViewModel by viewModels()
    private val sharedViewModel: SharedHomeViewModel by activityViewModels()

    private lateinit var participantAdapter: ParticipantAdapter
    private lateinit var currentUser: User
    private var groupChat: GroupChatListingData? = null
    private var participantsUpdateJob: Job? = null




    // Agora engine instance
    private var agoraEngine: RtcEngine? = null
    private var muteOn = false
    private var speakerOn = false
    private var inChannel = false
    private var isCurrentlySpeaking = false

    // Service variables
    private var voiceCallService: VoiceCallService? = null
    private var isServiceBound = false

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VoiceCallService.LocalBinder
            voiceCallService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            voiceCallService = null
            Log.d(TAG, "Service disconnected")
        }
    }


    // Broadcast receiver for call end from notification
    private val endCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Received END_CALL_FROM_NOTIFICATION broadcast")

            if (intent.action == VoiceCallService.ACTION_END_CALL_FROM_NOTIFICATION) {
                synchronized(this@VoiceCallFragment) {
                    if (inChannel && agoraEngine != null) {
                        try {
                            Log.d(TAG, "Leaving channel from notification broadcast")
                            // Leave the channel
                            agoraEngine?.leaveChannel()

                            // Update state
                            inChannel = false

                            // Update UI
                            activity?.runOnUiThread {
                                binding.rvParticipants.gone()
                                participantAdapter.submitList(mutableListOf())
                                callViewModel.joinedUsers.clear()
                                binding.tvCallStatus.text = "Call ended"

                                // Notify user
//                                Toast.makeText(context, "Call ended from notification", Toast.LENGTH_SHORT).show()

                                // Update Firebase or other backend
                                groupChat?.let { callViewModel.removeUser(it, currentUser) }
                                agoraEngine?.leaveChannel()
                                inChannel = false
                                stopCallService()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error leaving channel: ${e.message}", e)
                        }
                    } else {
                        Log.d(TAG, "Not in channel or engine is null: inChannel=$inChannel, engine=${agoraEngine != null}")
                    }
                }
            }
        }
    }

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check which permissions were granted and which were denied
        val deniedPermissions = permissions.filter { !it.value }.keys

        if (deniedPermissions.isEmpty()) {
            // All requested permissions were granted
            setupAndJoinChannel()
        } else {
            // Some permissions were denied
            Toast.makeText(
                requireContext(),
                "Some permissions were denied. Voice call functionality may be limited.",
                Toast.LENGTH_SHORT
            ).show()

            // Check if we have at least the critical permission (RECORD_AUDIO)
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED) {
                // We at least have microphone permission, so we can proceed
                setupAndJoinChannel()
            } else {
                // Can't proceed without microphone permission
                Toast.makeText(
                    requireContext(),
                    "Microphone permission is required for voice calls",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
        }
    }

    // Agora event handler
    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "Successfully joined channel: $channel, uid: $uid")
            inChannel = true
            activity?.runOnUiThread {
                binding.tvCallStatus.text = "Connected to ${groupChat?.groupName ?: "Group Call"}"

                // Update notification
                groupChat?.groupName?.let {
                    updateServiceNotification("Connected to: $it")
                }

                // Start foreground service
                startCallService()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "User offline: $uid, reason: $reason")
            activity?.runOnUiThread {
                /*callViewModel.onUserLeft(uid.toString())*/
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            Log.d(TAG, "Left channel")
            activity?.runOnUiThread {
                inChannel = false
                binding.rvParticipants.gone()
                participantAdapter.submitList(mutableListOf())
                callViewModel.joinedUsers.clear()
                binding.tvCallStatus.text = "Call ended"
//                groupChat?.let { callViewModel.removeUser(it, currentUser) }
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.d(TAG, "Connection state changed: $state, reason: $reason")
            if (state == io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTED &&
                reason == io.agora.rtc2.Constants.CONNECTION_CHANGED_INTERRUPTED) {
                enableMicrophone()
            }
        }

        override fun onAudioVolumeIndication(
            speakers: Array<out AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            activity?.runOnUiThread {
                if (speakers != null && callViewModel.joinedUsers.isNotEmpty()) {
                    speakers.forEach { speaker ->
                        val isSpeaking = speaker.volume > 10

                        // For local user, use the known local uid
                        if (speaker.uid == 0) {
                            if (!muteOn) {
                                callViewModel.speakingMap[myUid] = isSpeaking
                                Log.d(TAG, "Local user speaking: $isSpeaking, volume: ${speaker.volume}")
                            }
                        } else {
                            // For remote users, use the uid directly
                            callViewModel.speakingMap[speaker.uid] = isSpeaking
                            Log.d(TAG, "Remote user ${speaker.uid} speaking: $isSpeaking, volume: ${speaker.volume}")
                        }
                    }
                }
                val updatedList = callViewModel.joinedUsers.map { participant ->
                    // Get the Agora ID for this participant

                    val isSpeaking = callViewModel.speakingMap[participant.agoraUserId] ?: false

                    // Create new participant with updated speaking status
                    participant.copy(speakerOn = isSpeaking)
                }
                if (updatedList != participantAdapter.currentList) {
                    participantAdapter.submitList(updatedList)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val groupChatObjAsString = arguments?.getString(Constants.CLICKED_GROUP)
        groupChat = groupChatObjAsString?.let {
            Json.decodeFromString(GroupChatListingData.serializer(), it)
        }
        channelName = groupChat?.groupId
        token = arguments?.getString(Constants.TEMP_AGORA_TOKEN)
        currentUser = sharedViewModel.userData ?: User()
        myUid = currentUser.agoraUserId!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVoiceCallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize UI components
        setupUI()

        observeParticipantChanges()

        // Restore state if returning from background
        if (savedInstanceState != null) {
            inChannel = savedInstanceState.getBoolean("inChannel", false)
            myUid = savedInstanceState.getInt("myUid", 0)
            muteOn = savedInstanceState.getBoolean("isMuted", false)
            speakerOn = savedInstanceState.getBoolean("isSpeakerOn", false)

            // Update UI for restored state
            updateMuteButtonUI()
            updateSpeakerButtonUI()

            if (inChannel) {
                binding.tvCallStatus.text = "Reconnecting..."
            }
        }

        // Check permissions
        checkPermissions()

        // Register broadcast receiver
        val intentFilter = IntentFilter(VoiceCallService.ACTION_END_CALL_FROM_NOTIFICATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(
                endCallReceiver,
                intentFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
           registerReceiver(
                requireContext(),
                endCallReceiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        Log.d(TAG, "Registered END_CALL_FROM_NOTIFICATION receiver")


    }

    private fun observeParticipantChanges() {

        // Cancel any existing job
        participantsUpdateJob?.cancel()

        // Start new collection
        participantsUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                channelName?.let {
                    callViewModel.getCallParticipantsUpdates(it).collect { result ->
                        when (result) {
                            is NetworkResult.Success -> {
                                val call = result.data
                                // Update UI with new participant data
                                if (call != null) {
                                    updateParticipantsUI(call.participants)
                                }
                            }

                            is NetworkResult.Error -> {
                                // Handle error
                                Toast.makeText(requireContext(),
                                    "Error: ${result.message}",
                                    Toast.LENGTH_SHORT).show()
                            }

                            is NetworkResult.Loading -> {
                                // Show loading state if needed
                            }
                        }
                    }
                }
            }
        }

        callViewModel.removeUserLiveData.observe(viewLifecycleOwner) { consumableValue ->
            consumableValue.handle { response->
                when(response){
                    is NetworkResult.Success ->{
                        findNavController().popBackStack()
//                        Toast.makeText(context, "User Left call!", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(context, "Error : ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {
//                        Toast.makeText(context, "Processing..", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        callViewModel.updateUserCallStatusLiveData.observe(viewLifecycleOwner) { consumableValue ->
            consumableValue.handle { response->
                when(response){
                    is NetworkResult.Success ->{
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(context, "Error : ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {
                    }
                }
            }
        }
    }

    private fun updateParticipantsUI(participants: Map<String, CallParticipant>) {
        // Convert map to list and sort by join time (most recent first)
        val participantsList = participants.values.toMutableList().sortedBy { it.userName }

        // Update your local list
        callViewModel.joinedUsers.clear()
        callViewModel.joinedUsers.addAll(participantsList)
        Log.d(TAG, "Updated participants list: $participantsList")
        // First set all to not speaking
        callViewModel.speakingMap.clear()
        callViewModel.joinedUsers.forEach { participant ->
            val agoraId = participant.agoraUserId?:0
            callViewModel.speakingMap[agoraId] = false
        }

        // Submit to adapter
        participantAdapter.submitList(participantsList)

    }


    override fun onStart() {
        super.onStart()

        // Bind to the service if it's running
        activity?.let { activity ->
            Intent(activity, VoiceCallService::class.java).also { intent ->
                activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("inChannel", inChannel)
        outState.putInt("myUid", myUid)
        outState.putBoolean("isMuted", muteOn)
        outState.putBoolean("isSpeakerOn", speakerOn)
    }

    override fun onResume() {
        super.onResume()
        if (inChannel) {
            // Reconnect if needed
            if (agoraEngine == null) {
                setupAgoraEngine()
                reconnectToChannel()
            }

            // Ensure microphone is enabled
            enableMicrophone()
        }
    }

    override fun onPause() {
        super.onPause()

        // Keep audio active in background
        agoraEngine?.let {
            // Turn off speaker when app is in background
            if (speakerOn) {
                it.setEnableSpeakerphone(false)
            }

            // Ensure microphone is still publishing
            val options = ChannelMediaOptions().apply {
                publishMicrophoneTrack = !muteOn
                clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
            }
            it.updateChannelMediaOptions(options)
        }
    }

    private fun setupUI() {
        // Set up group name
        binding.tvCallStatus.text = "Connecting to ${groupChat?.groupName ?: "Group Call"}"

        // Set up participants adapter
        participantAdapter = ParticipantAdapter()
        binding.rvParticipants.apply {
            adapter = participantAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
        updateSpeakerButtonUI()

        // Set up call control buttons
        binding.btnMute.setOnClickListener {
            toggleMute()
        }

        binding.btnSpeaker.setOnClickListener {
            toggleSpeaker()
        }

        binding.btnEndCall.setOnClickListener {
            endCall()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showEndCallConfirmationDialog()
        }
    }

    private fun showEndCallConfirmationDialog() {
        val dialogListener = object : DialogUtils.DialogButtonClickListener {
            override fun onPositiveButtonClickedData(data: String) {
            }

            override fun onPositiveButtonClicked() {
                if (inChannel) {
                    endCall()
                }
            }

            override fun onNegativeButtonClicked() {

            }

            override fun onCloseButtonClicked() {

            }
        }

        DialogUtils.showDeleteDetailsDialog(
            requireContext(),
            dialogListener,
            getString(R.string.are_you_sure_you_want_to_end_this_call),
            getString(R.string.yes_end),
            getString(R.string.no_cancel)
        )
    }

    private fun checkPermissions() {
        val neededPermissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )

        // Add Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Separate permissions into three categories
        val grantedPermissions = mutableListOf<String>()
        val deniedPermissions = mutableListOf<String>() // Previously denied
        val permissionsToRequest = mutableListOf<String>() // Never asked or don't ask again

        // Check each permission's status
        for (permission in neededPermissions) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), permission) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    grantedPermissions.add(permission)
                }
                ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission) -> {
                    // Permission was previously denied but "Don't ask again" wasn't selected
                    deniedPermissions.add(permission)
                }
                else -> {
                    // Permission has never been asked for, or "Don't ask again" was selected
                    permissionsToRequest.add(permission)
                }
            }
        }

        when {
            // All permissions granted
            deniedPermissions.isEmpty() && permissionsToRequest.isEmpty() -> {
                Log.d(TAG, "All permissions granted")
                setupAndJoinChannel()
            }
            // Some permissions were denied before
            deniedPermissions.isNotEmpty() -> {
                // Show explanation for each denied permission
                showPermissionExplanationDialog(deniedPermissions, permissionsToRequest)
            }
            // Some permissions have never been requested or "Don't ask again" was selected
            permissionsToRequest.isNotEmpty() -> {
                Log.d(TAG, "Requesting permissions: $permissionsToRequest")
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    private fun showPermissionExplanationDialog(deniedPermissions: List<String>, permissionsToRequest: List<String>) {
        // Create a readable list of what permissions are needed
        val permissionMessages = mutableListOf<String>()

        if (deniedPermissions.contains(Manifest.permission.RECORD_AUDIO)) {
            permissionMessages.add("• Microphone access is needed to make voice calls")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            deniedPermissions.contains(Manifest.permission.BLUETOOTH_CONNECT)) {
            permissionMessages.add("• Bluetooth permission is needed for headset support")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            deniedPermissions.contains(Manifest.permission.POST_NOTIFICATIONS)) {
            permissionMessages.add("• Notification permission is needed for call notifications")
        }

        val message = "The following permissions are required:\n\n" +
                permissionMessages.joinToString("\n") +
                "\n\nPlease grant these permissions to continue."

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Request Again") { _, _ ->
                // Request the denied permissions again
                if (deniedPermissions.isNotEmpty()) {
                    requestPermissionLauncher.launch(deniedPermissions.toTypedArray())
                }
            }
            .setNegativeButton("Open Settings") { _, _ ->
                // Direct to app settings for permissions that can't be requested directly
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNeutralButton("Cancel") { _, _ ->
                // Go back to previous screen
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupAndJoinChannel() {
        setupAgoraEngine()

        // Join the channel
        Log.d(TAG, "token: $token")
        token?.let { safeToken ->
            Log.d(TAG, "token: $safeToken")
            channelName?.let {
                Log.d(TAG, "channel: $it")
                joinChannel(it, safeToken) }
        } ?: run {
            // If token is null, try to get it from ViewModel
            /*callViewModel.getUserCallToken(groupId)*/
            //Observer mai
            /*if (newToken.isNotEmpty()) {
                token = newToken
                joinChannel(groupId, newToken)
            } else {
                showToast("Failed to get token")
                findNavController().popBackStack()
            }*/
        }
    }

    private fun setupAgoraEngine() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = requireContext()
                mAppId = myAppId
                mEventHandler = rtcEventHandler
            }

            agoraEngine = RtcEngine.create(config)

            // Set audio profile for better voice quality
            agoraEngine?.setAudioProfile(
                io.agora.rtc2.Constants.AUDIO_PROFILE_DEFAULT,
                io.agora.rtc2.Constants.AUDIO_SCENARIO_CHATROOM
            )
            agoraEngine?.setEnableSpeakerphone(speakerOn)

            // Enable audio volume indicator
            agoraEngine?.enableAudioVolumeIndication(500, 3, true)

            Log.d(TAG, "Agora engine initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Agora SDK: ${e.message}", e)
            showToast("Failed to initialize Agora SDK: ${e.message}")
            findNavController().popBackStack()
        }
    }

    private fun joinChannel(channelName: String, token: String) {


        val options = ChannelMediaOptions().apply {
            autoSubscribeAudio = true
            publishMicrophoneTrack = true
            clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION
        }

        // Use user ID from profile, fallback to 0 if not available
        Log.d(TAG, "Joining channel: $channelName with uid: ${myUid}")

        try {
            agoraEngine?.setEnableSpeakerphone(speakerOn)
            agoraEngine?.joinChannel(token, channelName, myUid, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join channel: ${e.message}", e)
            showToast("Failed to join call: ${e.message}")
            findNavController().popBackStack()
        }
    }
    private fun toggleMute() {
        muteOn = !muteOn
        agoraEngine?.muteLocalAudioStream(muteOn)
        updateMuteButtonUI()

        // Update current user's mute status in the participants list
        /*callViewModel.updateParticipantMuteStatus(currentUser.userId ?: "", muteOn)*/
        channelName?.let {
            currentUser.userId?.let { it1 ->
                callViewModel.updateUserCallStatus(it,
                    it1, isMuted = muteOn,null)
            }
        }

        showToast(if (muteOn) "Microphone muted" else "Microphone unmuted")
    }

    private fun updateMuteButtonUI() {
        binding.btnMute.setImageResource(
            if (muteOn) R.drawable.ic_mic_off
            else R.drawable.ic_mic
        )
    }

    private fun toggleSpeaker() {
        speakerOn = !speakerOn
        agoraEngine?.setEnableSpeakerphone(speakerOn)
        updateSpeakerButtonUI()

        showToast(if (speakerOn) "Speaker on" else "Speaker off")
    }

    private fun updateSpeakerButtonUI() {
        binding.btnSpeaker.setImageResource(
            if (speakerOn) R.drawable.ic_speaker
            else R.drawable.ic_speaker_off
        )
    }

    private fun endCall() {
        if (inChannel) {
            agoraEngine?.leaveChannel()
            inChannel = false
            stopCallService()
            groupChat?.let { callViewModel.removeUser(it, currentUser) }
        }
    }

    private fun startCallService() {
        Log.d(TAG, "Starting call service")

        val serviceIntent = Intent(requireContext(), VoiceCallService::class.java).apply {
            action = VoiceCallService.ACTION_START_CALL
            putExtra(VoiceCallService.EXTRA_CHANNEL_NAME, groupChat?.groupName ?: "Group Call")
        }

        // Start service
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }

            // Bind to service
            requireActivity().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service: ${e.message}")
        }
    }

    private fun stopCallService() {
        Log.d(TAG, "Stopping call service")

        val serviceIntent = Intent(requireContext(), VoiceCallService::class.java).apply {
            action = VoiceCallService.ACTION_END_CALL
        }

        requireContext().startService(serviceIntent)

        // Unbind from service
        if (isServiceBound) {
            try {
                requireActivity().unbindService(serviceConnection)
                isServiceBound = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding service: ${e.message}")
            }
        }
    }

    private fun updateServiceNotification(message: String) {
        if (isServiceBound && voiceCallService != null) {
            Log.d(TAG, "Updating notification: $message")
            voiceCallService?.updateNotification(message)
        } else {
            Log.d(TAG, "Can't update notification - service not bound")
        }
    }

    private fun enableMicrophone() {
        agoraEngine?.let {
            // Respect user's mute setting
            it.muteLocalAudioStream(muteOn)

            val options = ChannelMediaOptions().apply {
                publishMicrophoneTrack = !muteOn
                clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
            }
            it.updateChannelMediaOptions(options)
        }
    }


    private fun reconnectToChannel() {
        Log.d(TAG, "Attempting to reconnect to channel")

        if (agoraEngine == null) {
            setupAgoraEngine()
        }

        binding.tvCallStatus.text = "Reconnecting..."

        val options = ChannelMediaOptions().apply {
            clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_COMMUNICATION
            publishMicrophoneTrack = !muteOn
            autoSubscribeAudio = true
        }

        // Use saved token and channel name
        token?.let { safeToken ->
            channelName?.let { safeChannelName ->
                try {
                    agoraEngine?.joinChannel(safeToken, safeChannelName, myUid, options)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reconnect: ${e.message}", e)
                    showToast("Failed to reconnect to call")
                }
            }
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()

        // Unbind from service but keep it running
        if (isServiceBound) {
            try {
                requireActivity().unbindService(serviceConnection)
                isServiceBound = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding service: ${e.message}")
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister broadcast receiver
        try {
            requireActivity().unregisterReceiver(endCallReceiver)
            Log.d(TAG, "Unregistered END_CALL_FROM_NOTIFICATION receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }

        // Clean up Agora if we're still in a call
        if (inChannel) {
            try {
                agoraEngine?.leaveChannel()
                inChannel = false
            } catch (e: Exception) {
                Log.e(TAG, "Error leaving channel: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Final cleanup
        try {
            RtcEngine.destroy()
            agoraEngine = null
            Log.d(TAG, "Agora engine destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying Agora engine: ${e.message}")
        }
    }

    // Extension function to convert User to CallParticipant
    private fun User.toCallParticipant(): CallParticipant {
        return CallParticipant(
            userId = this.userId ?: "",
            userName = this.userName ?: "Unknown User",
            userProfileImgUrl = this.userProfileImgUrl,
            joinedAt = System.currentTimeMillis(),
            muteOn = false,
            speakerOn = false
        )
    }
}
