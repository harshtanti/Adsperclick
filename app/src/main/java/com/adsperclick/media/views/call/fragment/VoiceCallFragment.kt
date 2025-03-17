package com.adsperclick.media.views.call.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.data.dataModels.CallParticipant
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentVoiceCallBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.views.call.adapters.ParticipantAdapter
import com.adsperclick.media.views.call.viewmodel.CallViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants.USER_OFFLINE_QUIT
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class VoiceCallFragment : Fragment() {

    private lateinit var binding: FragmentVoiceCallBinding
    private val callViewModel: CallViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var participantAdapter: ParticipantAdapter
    private lateinit var currentUser: User
    private var groupChat: GroupChatListingData? = null

    // Agora engine instance
    private var agoraEngine: RtcEngine? = null
    private var isMuted = false
    private var isSpeakerOn = false
    private var isJoined = false

    // Permission launcher
    /*private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            setupAndJoinChannel()
        } else {
            Toast.makeText(
                requireContext(),
                "Voice call permissions are required to make a call",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        }
    }*/



    /*private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            myUid = uid
            isInChannel = true
            activity?.runOnUiThread {
                connectionStatusText.text = "Connected to channel: $channel"
                connectionStatusText.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.green))
                addUserToDisplay(uid, true)
                updateServiceNotification("Connected to channel: $channel")
            }
        }

        *//*override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "User joined: $uid", Toast.LENGTH_SHORT).show()
                addUserToDisplay(uid, false)
                updateServiceNotification("Call in progress: ${joinedUsers.size} participants")
            }
        }*//*

        *//*override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "User offline: $uid", Toast.LENGTH_SHORT).show()
                removeUserFromDisplay(uid)
            }
        }*//*

        override fun onLeaveChannel(stats: RtcStats?) {
            super.onLeaveChannel(stats)
            activity?.runOnUiThread {
                isInChannel = false
                userListLayout.removeAllViews()
                joinedUsers.clear()
                connectionStatusText.text = "Disconnected"
                connectionStatusText.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.red))
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            super.onConnectionStateChanged(state, reason)
            if (state == io.agora.rtc2.Constants.CONNECTION_STATE_CONNECTED &&
                reason == io.agora.rtc2.Constants.CONNECTION_CHANGED_INTERRUPTED) {
                enableMicrophone()
            }
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val groupChatObjAsString = arguments?.getString(Constants.CLICKED_GROUP)
        groupChat = groupChatObjAsString?.let {
            Json.decodeFromString(GroupChatListingData.serializer(), it)
        }

        currentUser = tokenManager.getUser()!!
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

        initializeCall()

        setupUI()
        setupObservers()

        // Check and request permissions
        /*checkPermissions()*/
    }

    private fun initializeCall(){
        /*callViewModel.initializeCall()*/
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

        /*// Set up call control buttons
        binding.btnMute.setOnClickListener {
            toggleMute()
        }

        binding.btnSpeaker.setOnClickListener {
            toggleSpeaker()
        }

        binding.btnEndCall.setOnClickListener {
            endCall()
        }*/
    }

    private fun setupObservers() {
       /* callViewModel.participants.observe(viewLifecycleOwner) { participants ->
            participantAdapter.submitList(participants)
        }*/
    }

    /*private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            setupAndJoinChannel()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun setupAndJoinChannel() {
        setupAgoraEngine()

        val groupId = groupChat?.groupId ?: return

        // Start the call in Firebase
        *//*callViewModel.startCall(groupId)*//*

        // Get temporary token and join channel
       *//* val token = callViewModel.getAgoraToken()*//*
       *//* joinChannel(groupId, token)*//*
    }*/

    /*private fun setupAgoraEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = Constants.AGORA_APP_ID
            config.mEventHandler = rtcEventHandler

            agoraEngine = RtcEngine.create(config)

            // Enable audio volume indicator
            agoraEngine?.enableAudioVolumeIndication(500, 3, true)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to initialize Agora SDK: ${e.message}", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }*/
    }

    /*private fun joinChannel(channelName: String, token: String) {
        // Configure channel options
        val options = ChannelMediaOptions().apply {
            autoSubscribeAudio = true
            clientRoleType = io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER
        }

        // Join the channel
        val uid = currentUser.userId?.toIntOrNull() ?: 0
        agoraEngine?.joinChannel(token, channelName, uid, options)
    }

    private fun toggleMute() {
        isMuted = !isMuted
        agoraEngine?.muteLocalAudioStream(isMuted)

        binding.btnMute.setImageResource(
            if (isMuted) R.drawable.ic_mic_off
            else R.drawable.ic_mic
        )

        // Update current user's mute status in the participants list
        callViewModel.updateParticipantMuteStatus(currentUser.userId ?: "", isMuted)
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        agoraEngine?.setEnableSpeakerphone(isSpeakerOn)

        binding.btnSpeaker.setImageResource(
            if (isSpeakerOn) R.drawable.ic_speaker
            else R.drawable.ic_speaker_off
        )
    }

    private fun endCall() {
        agoraEngine?.leaveChannel()

        val groupId = groupChat?.groupId ?: return
        callViewModel.endCall(groupId)

        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (isJoined) {
            endCall()
        }

        // Clean up resources
        RtcEngine.destroy()
        agoraEngine = null
    }

    // Extension function to convert User to CallParticipant
    private fun User.toCallParticipant(): CallParticipant {
        return CallParticipant(
            userId = this.userId ?: "",
            userName = this.userName ?: "Unknown User",
            userProfileImgUrl = this.userProfileImgUrl,
            joinedAt = System.currentTimeMillis(),
            isMuted = false,
            isSpeaking = false,
            isActive = true
        )
    }*/
/*}*/
