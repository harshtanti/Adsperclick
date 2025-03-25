package com.adsperclick.media.views.chat.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.adsperclick.media.R
import com.adsperclick.media.applicationCommonView.TokenManager
import com.adsperclick.media.applicationCommonView.bottomsheet.UploadImageDocsBottomSheet
import com.adsperclick.media.data.dataModels.GroupChatListingData
import com.adsperclick.media.data.dataModels.Message
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.User
import com.adsperclick.media.databinding.FragmentMessagingBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.Constants.BOTTOM_MOST_MSG
import com.adsperclick.media.utils.Constants.CLICKED_GROUP
import com.adsperclick.media.utils.Constants.LAST_SEEN_GROUP_TIME
import com.adsperclick.media.utils.Constants.LIMIT_MSGS
import com.adsperclick.media.utils.Constants.MSG_TYPE.IMG_URL
import com.adsperclick.media.utils.Constants.MSG_TYPE.MEDIATOR_ANNOUNCEMENT
import com.adsperclick.media.utils.Constants.MSG_TYPE.PDF_DOC
import com.adsperclick.media.utils.Constants.MSG_TYPE.VIDEO
import com.adsperclick.media.utils.Constants.TOP_MOST_MSG
import com.adsperclick.media.utils.DialogUtils
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.chat.adapters.MessagesAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.adsperclick.media.views.homeActivity.SharedHomeViewModel
import com.google.firebase.database.ChildEventListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint
class MessagingFragment : Fragment(),View.OnClickListener {

    lateinit var binding: FragmentMessagingBinding
    private val chatViewModel: ChatViewModel by viewModels()
    private val sharedViewModel: SharedHomeViewModel by activityViewModels()
    private var showCallDialog = true       // We only show it once

    var loadingBottomMsgs = false

    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var adapter : MessagesAdapter

    private var messagesListener: ChildEventListener? = null

    private lateinit var currentUser: User
    private var groupChat :GroupChatListingData? = null
    private lateinit var listofGroupMemberId: List<String>
    private var lastMsgInGroup : Message?= null
    var lastTimeVisitedThisGroupTimestamp :Long?= null

    private val bottomSheetSelectables = arrayListOf(Constants.CLOSE_VISIBLE,Constants.HEADING_VISIBLE,Constants.CAMERA_VISIBLE,
        Constants.GALLERY_VISIBLE, Constants.VIDEO_VISIBLE, Constants.PDF_VISIBLE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupChatObjAsString = arguments?.getString(CLICKED_GROUP)
        groupChat = groupChatObjAsString?.let {
                Json.decodeFromString(GroupChatListingData.serializer(), it)
            }

        lastTimeVisitedThisGroupTimestamp = arguments?.getLong(LAST_SEEN_GROUP_TIME)
        currentUser = sharedViewModel.userData ?: User()      // Empty "User" data-class if not found, everything is null by default
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMessagingBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpView()
        setupAdapter()
        setUpListener()
        setupObservers()
    }

    private fun setUpView(){
             // Passed from ChatFragment



        binding.includeTopBar.tvGroupName.text = groupChat?.groupName ?: "Group-Name"

        groupChat?.groupImgUrl?.let { imageUrl ->
            UtilityFunctions.loadImageWithGlide(
                binding.includeTopBar.imgProfileDp.context,
                binding.includeTopBar.imgProfileDp,
                imageUrl
            )
        } ?: run {
            UtilityFunctions.setInitialsDrawable(
                binding.includeTopBar.imgProfileDp,
                groupChat?.groupName
            )
        }

        groupChat?.groupId?.let { groupId->
            sharedViewModel.pageNo?.let {
                turnOnReadingMode()

            } ?: run{
                chatViewModel.setGroupId(groupId)   // Group Id is set, "messages" live-data gets active,
                // and it starts listening for realtime updates in room-db, realtime room-db updates are sent to adapter via observer here
                chatViewModel.fetchAllNewMessages(groupId)      // To fetch all the unread messages for this group in one single go!
            }

            chatViewModel.isCallOngoing(groupId)
        }

        listofGroupMemberId = groupChat?.listOfUsers?.map { it.userId } ?: emptyList()
    }



    private fun setupAdapter(){

        val isClientOnRight = currentUser.role == Constants.ROLE.CLIENT
        adapter = currentUser.userId?.let { MessagesAdapter(it, isClientOnRight, onMessageClickListener) }!!
        binding.rvChat.adapter = adapter
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
            /*reverseLayout = true */ // Reverse layout for better pagination
        }
        adapter.updateLastVisitedTimestamp(lastTimeVisitedThisGroupTimestamp)
    }



    private fun setUpListener(){

        binding.includeTextSender.btnSendMsg.setOnClickListener(this)
        binding.includeTextSender.btnShareMedia.setOnClickListener(this)
        binding.includeTextSender.btnEmoji.setOnClickListener(this)
        binding.includeTopBar.container.setOnClickListener(this)
        binding.includeTopBar.btnBack.setOnClickListener(this)
        binding.includeTopBar.btnCall.setOnClickListener(this)
        binding.includeTopBar.btnCallLottie.setOnClickListener(this)
        binding.includeTopBar.btnVideoCall.setOnClickListener(this)
        binding.includeTextSender.btnGoToRecentMsgs.setOnClickListener(this)
    }

    private fun setupObservers(){

        chatViewModel.imageUploadedLiveData.observe(viewLifecycleOwner){ consumableValue ->
            consumableValue.handle { response->
                // Response is a string "image download url", we're not using it here
                when(response){
                    is NetworkResult.Success->{}
                    is NetworkResult.Error->{
                        Toast.makeText(context, "Error : ${response.message}", Toast.LENGTH_SHORT).show()
                    }

                    is NetworkResult.Loading -> {
                        Toast.makeText(context, "Sharing..", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // To update adapter with new messages, whenever Room-DB is updated
        chatViewModel.messages.observe(viewLifecycleOwner) { consumableValue->

            consumableValue.handle {response ->

                val oldLastMessagePosition = adapter.itemCount - 1
                lastMsgInGroup = response.firstOrNull()
                chatViewModel.checkIfLastMsgRelatedToCall(response.firstOrNull())

                val modifiedResponse = response.toMutableList().apply {
                    if(response.size == LIMIT_MSGS) {add(Constants.READING_MODE_MSG)}
                    reverse()
                }.toList() // Convert back to List<T>

                adapter.submitList(modifiedResponse) {

//                (binding.rvChat.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, offset)

                    if(sharedViewModel.lastScrollPosition != -1 && sharedViewModel.lastScrollOffset != -1){
                        (binding.rvChat.layoutManager as LinearLayoutManager)
                            .scrollToPositionWithOffset(sharedViewModel.lastScrollPosition, sharedViewModel.lastScrollOffset)
                        sharedViewModel.saveScrollPosition(-1,-1)
                    }else {
                        binding.rvChat.scrollToPosition(adapter.itemCount - 1)
                    }

                    // Ensure previous last message updates (To update the chat bubble of previous message
                    if (oldLastMessagePosition >= 0) {
                        adapter.notifyItemChanged(oldLastMessagePosition)
                    }
                }
            }
        }

        sharedViewModel.msgsLiveData.observe(viewLifecycleOwner) { consumableValue ->
            consumableValue.handle { response ->
                when (response) {
                    is NetworkResult.Error -> {
                        Toast.makeText(context, "Error : ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> TODO()
                    is NetworkResult.Success -> {
                        val msgs = response.data

                        val modifiedRes = msgs?.let {
                            it.toMutableList().apply {
                                if(sharedViewModel.pageNo != 0) add(0, BOTTOM_MOST_MSG)
                                if(it.size >= LIMIT_MSGS) {
                                    add(TOP_MOST_MSG)
                                }
                                reverse()
                            }.toList()
                        }

                        adapter.submitList(modifiedRes) {
                            if(loadingBottomMsgs){
                                loadingBottomMsgs = false
                                binding.rvChat.scrollToPosition((LIMIT_MSGS/2)-5)
                            }

                            if(sharedViewModel.lastScrollPosition != -1 && sharedViewModel.lastScrollOffset != -1){
                                (binding.rvChat.layoutManager as LinearLayoutManager)
                                    .scrollToPositionWithOffset(sharedViewModel.lastScrollPosition, sharedViewModel.lastScrollOffset)
                                sharedViewModel.saveScrollPosition(-1,-1)
                            }
                        }
                    }
                }
            }
        }

        chatViewModel.getAgoraTokenLiveData.observe(viewLifecycleOwner) { consumableValue ->
            consumableValue.handle { response->

                when(response){
                    is NetworkResult.Success ->{
                        val bundle = Bundle().apply {
                            groupChat?.let { gc ->
                                val gcObjAsString = Json.encodeToString(GroupChatListingData.serializer(), gc)
                                putString(CLICKED_GROUP, gcObjAsString)
                            }
                            putString(Constants.TEMP_AGORA_TOKEN, response.data)
                        }
                        findNavController().navigate(R.id.action_messagingFragment_to_voiceCallFragment, bundle)
                    }

                    is NetworkResult.Error -> {
                        Toast.makeText(context, "Error : ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {
                        Toast.makeText(context, "Calling..", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        chatViewModel.userLeftCallLiveData.observe(viewLifecycleOwner) { consumableValue ->
            consumableValue.handle { response->
                when(response){
                    is NetworkResult.Success ->{}
                    is NetworkResult.Error -> {
                        Toast.makeText(context, "Error : ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {}
                }
            }
        }

        chatViewModel.isCallOngoingLiveData.observe(viewLifecycleOwner) { consumableValue ->
            consumableValue.handle { response->
                when(response){
                    is NetworkResult.Success ->{
                        if(response.data == true){
                            handleCallIcon(true)
                            if(showCallDialog && lastMsgInGroup?.senderId != currentUser.userId){
                                showJoinCallDialog()
                            }
                        } else {
                            handleCallIcon(false)
                        }
                    }

                    is NetworkResult.Error -> {
                        Toast.makeText(context, "Error : ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {
                        Toast.makeText(context, "Processing..", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkCallPermissions(callback: (Boolean) -> Unit) {
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

        // Separate permissions into categories
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
                callback(true)
            }
            // Some permissions were denied before
            deniedPermissions.isNotEmpty() -> {
                // Show explanation for each denied permission
                showPermissionExplanationDialog(deniedPermissions, permissionsToRequest, callback)
            }
            // Some permissions have never been requested or "Don't ask again" was selected
            permissionsToRequest.isNotEmpty() -> {
                requestCallPermissions(permissionsToRequest.toTypedArray(), callback)
            }
        }
    }

    private fun showPermissionExplanationDialog(
        deniedPermissions: List<String>,
        permissionsToRequest: List<String>,
        callback: (Boolean) -> Unit
    ) {
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
                    requestCallPermissions(deniedPermissions.toTypedArray(), callback)
                }
            }
            .setNegativeButton("Open Settings") { _, _ ->
                // Direct to app settings for permissions that can't be requested directly
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
                // Return false since we don't know if they'll grant permissions in settings
                callback(false)
            }
            .setNeutralButton("Cancel") { _, _ ->
                // User canceled, don't proceed with call
                callback(false)
            }
            .setCancelable(false)
            .show()
    }

    // Add this at the class level
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // All microphone permission is essential
        val microphoneGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

        callPermissionCallback?.invoke(microphoneGranted)
    }

    // Add this property at the class level
    private var callPermissionCallback: ((Boolean) -> Unit)? = null

    private fun requestCallPermissions(permissions: Array<String>, callback: (Boolean) -> Unit) {
        // Store callback to be used when permission result comes back
        callPermissionCallback = callback
        requestPermissionLauncher.launch(permissions)
    }


    override fun onPause() {
        super.onPause()

        currentUser.userId?.let { userId->
            sharedViewModel.lastSeenTimeForEachUserEachGroup?.get(groupChat?.groupId.toString())?.set(userId,
                UtilityFunctions.getTime()
            )
        }

        groupChat?.let { gc->
            gc.listOfUsers?.let {listOfGroupMembers->
                lastMsgInGroup?.msgId?.let{ idOfLastMsg ->
                    gc.groupId?.let { groupId ->
                        currentUser.userId?.let {userId->
                            chatViewModel.updateLastReadMsg(groupId, idOfLastMsg, userId, listOfGroupMembers)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatViewModel.stopRealtimeListening()
    }

    fun turnOnReadingMode(){
        chatViewModel.stopRealtimeListening()
        binding.includeTextSender.groupMsgSendingTemplate.gone()
        binding.includeTextSender.btnGoToRecentMsgs.visible()

        groupChat?.groupId?.let {
            sharedViewModel.getSpecifiedMessages(it)
        }
    }

    fun turnOffReadingMode(){
        groupChat?.groupId?.let {groupId->
            chatViewModel.setGroupId(groupId)
            chatViewModel.fetchAllNewMessages(groupId)
        }
        loadingBottomMsgs = false
        sharedViewModel.pageNo=null
        binding.includeTextSender.groupMsgSendingTemplate.visible()
        binding.includeTextSender.btnGoToRecentMsgs.gone()
    }





    override fun onClick(v: View?) {
        val imm = v?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        when(v){
            binding.includeTextSender.btnEmoji ->{
                with(binding.includeTextSender){
                    etTypeMessage.requestFocus()
                    imm.showSoftInput(etTypeMessage, InputMethodManager.SHOW_IMPLICIT)

                    // Simulate pressing the emoji key (works on Gboard)
                    val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_LANGUAGE_SWITCH)
                    etTypeMessage.dispatchKeyEvent(event)
                }
            }

            binding.includeTextSender.btnGoToRecentMsgs ->{
                turnOffReadingMode()
            }

            binding.includeTextSender.btnShareMedia ->{
                val bottomSheet = UploadImageDocsBottomSheet.createBottomsheet(
                    uploadOnSelectListener, bottomSheetSelectables,"Send")
                bottomSheet.show(childFragmentManager, bottomSheet.tag)
            }

            binding.includeTextSender.btnSendMsg -> {
                val text = binding.includeTextSender.etTypeMessage.text.toString()      // here "text" will have the text msg
                if(text.isNotEmpty()){
                    binding.includeTextSender.etTypeMessage.text.clear()
                    groupChat?.let {gc->

                        chatViewModel.sendMessage(text, gc.groupId ?: "",
                            currentUser, gc.groupName ?: "", listofGroupMemberId
                        )
                    }
                }
            }

            binding.includeTopBar.container ->{
                groupChat?.let {
                    val bundle = Bundle()
                    bundle.putString(CLICKED_GROUP, it.groupId)
                    findNavController().navigate(R.id.action_messagingFragment_to_groupProfileFragment, bundle)
                }
            }

            binding.includeTopBar.btnBack -> {
                findNavController().navigateUp()
            }

            binding.includeTopBar.btnCall, binding.includeTopBar.btnCallLottie ->{
                joinCall()
            }

            binding.includeTopBar.btnVideoCall->{
                Toast.makeText(context, "Feature not available yet!", Toast.LENGTH_SHORT).show()

//                groupChat?.let { gc ->
//                    lifecycleScope.launch {
//                        for (i in CC until CC + 30) {
//                            val text = "Message - $i"
//                            chatViewModel.sendMessage(text, gc.groupId ?: "",
//                                currentUser, gc.groupName ?: "", listofGroupMemberId
//                            )
//
//                            delay(300) // Wait for 500ms before sending the next message
//                        }
//                        CC += 30
//                    }
//                }
            }

//            binding.includeTopBar.btnCallEnd ->{
//                groupChat?.let { groupData->
//                    currentUser.let { userData->
//                        chatViewModel.LeaveCall(groupData , userData )
//                    }
//                }
//            }
        }
    }

    private val uploadOnSelectListener = object : UploadImageDocsBottomSheet.OnSelectListener{
        override fun onSelect(option: String, type: UploadImageDocsBottomSheet.UploadMethod) {

            if(option.isNotEmpty()){
                val imageFile = File(option)
                if(imageFile.exists()){
                    val msgType = when(type){
                        UploadImageDocsBottomSheet.UploadMethod.CAMERA,
                        UploadImageDocsBottomSheet.UploadMethod.GALLERY -> Constants.MSG_TYPE.IMG_URL

                        UploadImageDocsBottomSheet.UploadMethod.VIDEO_CAMERA,
                        UploadImageDocsBottomSheet.UploadMethod.VIDEO_GALLERY-> VIDEO

                        UploadImageDocsBottomSheet.UploadMethod.PDF -> Constants.MSG_TYPE.PDF_DOC
                        UploadImageDocsBottomSheet.UploadMethod.NOTSELECTED -> null
                    }


                    groupChat?.let {gc->
                        gc.groupId?.let {groupId->
                            gc.groupName?.let { groupName ->
                                msgType?.let {msgType->
                                    chatViewModel.uploadFile(groupId,
                                        groupName, imageFile,  listofGroupMemberId, msgType)
                                } } }       // This function will upload file in firebase-storage
                                            // and also display it to user in this fragment
                    }
                }
            }
        }
    }

    private val onMessageClickListener = object : MessagesAdapter.OnMessageClickListener{
        override fun onItemClick(message: Message) {

            if(message.msgType == MEDIATOR_ANNOUNCEMENT) {

                sharedViewModel.pageNo?.let {
                    sharedViewModel.pageNo = if(message.msgId == "topMost" ){
                        sharedViewModel.pageNo!! + 1
                    }
                    else {
                        loadingBottomMsgs = true
                        sharedViewModel.pageNo!! - 1
                    }

                    if(sharedViewModel.pageNo == -1) turnOffReadingMode()
                    else {
                        sharedViewModel.getSpecifiedMessages(groupChat?.groupId ?: "")
                    }
                } ?: run{
                    sharedViewModel.pageNo=1
                    turnOnReadingMode()
                }
                return
            }

            // Save current scroll position before navigating to some other fragment :) Shared view model will help us come back
            // exactly at that position :)
            val layoutManager = binding.rvChat.layoutManager as LinearLayoutManager
            val position = layoutManager.findFirstVisibleItemPosition()
            val firstView = layoutManager.findViewByPosition(position)
            val offset = firstView?.top ?: 0

            sharedViewModel.saveScrollPosition(position, offset)

            if(message.msgType == PDF_DOC){     // For PDF-Doc, we're moving to "PdfWebViewActivity"
                openPdfInWebView(message.message)       // For online viewing of all kinds of docs using "google's doc rendering tool"
                return
            }


            val fileName = if(message.msgType == IMG_URL) "Image" else "Video"
            // Navigate to preview fragment
            val bundle = Bundle().apply {
                putString("mediaUrl", message.message) // Your download URL
                putString("mediaType", message.msgType.toString()) // IMAGE, VIDEO, DOCUMENT etc.
                putString("fileName", fileName)
                // Any other metadata you want to pass
            }

            findNavController().navigate(
                R.id.action_messagingFragment_to_mediaPreviewFragment,
                bundle
            )
        }
    }

    fun handleCallIcon(isCallOngoing: Boolean){
        with(binding.includeTopBar){
            if(isCallOngoing){
                btnCall.gone()
                btnCallLottie.visible()
                UtilityFunctions.setupLottieAnimation("calling.json", btnCallLottie)
            } else{
                btnCall.visible()
                btnCallLottie.gone()
            }
        }
    }

    // To open PDF directly using firebase download URL (Firebase download url is a web-url link
    // as u can simply paste that link on web and can view an image or document, here we're using
    // this "PdfWebViewActivity" to view pdf using web-view, so we're not storing the doc on my device
    // just retrieving it using net whenever user wants to view it :)
    fun openPdfInWebView(downloadUrl: String?) {

        downloadUrl.let {downloadUrl->
            val intent = Intent(requireContext(), PdfWebViewActivity::class.java).apply {
                putExtra("pdf_url", downloadUrl)
            }
            startActivity(intent)
        }
    }

    private fun showJoinCallDialog() {
        val dialogListener = object : DialogUtils.DialogButtonClickListener {
            override fun onPositiveButtonClickedData(data: String) {

            }

            override fun onPositiveButtonClicked() {
                joinCall()
            }

            override fun onNegativeButtonClicked() {

            }

            override fun onCloseButtonClicked() {

            }
        }

        DialogUtils.showDeleteDetailsDialog(
            requireContext(),
            dialogListener,
            getString(R.string.call_in_progress),
            getString(R.string.yes_join),
            getString(R.string.no_join)
        )

        showCallDialog = false
    }

    fun joinCall(){
        // Check permissions first before attempting to get token or navigate
        checkCallPermissions { permissionsGranted ->
            if (permissionsGranted) {
                // Permissions are granted, proceed with call
                groupChat?.let { groupData ->
                    currentUser.let { userData ->
                        chatViewModel.getAgoraCallToken(groupData, userData)
                    }
                }
            }
            // If permissions not granted, the checkCallPermissions function will handle showing dialogs
        }
    }

}
