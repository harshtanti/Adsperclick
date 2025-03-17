package com.adsperclick.media.views.chat.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.adsperclick.media.utils.Constants.CLICKED_GROUP
import com.adsperclick.media.utils.Constants.LAST_SEEN_GROUP_TIME
import com.adsperclick.media.utils.Constants.MSG_TYPE.IMG_URL
import com.adsperclick.media.utils.Constants.MSG_TYPE.PDF_DOC
import com.adsperclick.media.utils.Constants.MSG_TYPE.VIDEO
import com.adsperclick.media.utils.Constants.PDF_VISIBLE
import com.adsperclick.media.utils.Constants.VIDEO_VISIBLE
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.views.chat.adapters.MessagesAdapter
import com.adsperclick.media.views.chat.viewmodel.ChatViewModel
import com.google.firebase.database.ChildEventListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint
class MessagingFragment : Fragment(),View.OnClickListener {

    lateinit var binding: FragmentMessagingBinding
    private val chatViewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var adapter : MessagesAdapter

    private var messagesListener: ChildEventListener? = null

    private lateinit var currentUser: User
    private var groupChat :GroupChatListingData? = null
    private lateinit var listofGroupMemberId: List<String>
    private var idOfLastMsgInGroup : String?= null
    var lastTimeVisitedThisGroupTimestamp :Long?= null

    val bottomSheetSelectables = arrayListOf(Constants.CAMERA_VISIBLE,
        Constants.GALLERY_VISIBLE, VIDEO_VISIBLE, PDF_VISIBLE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupChatObjAsString = arguments?.getString(CLICKED_GROUP)
        groupChat = groupChatObjAsString?.let {
                Json.decodeFromString(GroupChatListingData.serializer(), it)
            }

        lastTimeVisitedThisGroupTimestamp = arguments?.getLong(LAST_SEEN_GROUP_TIME)
        currentUser = tokenManager.getUser() ?: User()      // Empty "User" data-class if not found, everything is null by default
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
            chatViewModel.setGroupId(groupId)   // Group Id is set, "messages" live-data gets active, and it starts listening for realtime updates in room-db, realtime room-db updates are sent to adapter via observer here
            chatViewModel.fetchAllNewMessages(groupId)      // To fetch all the unread messages for this group in one single go!
        }

        listofGroupMemberId = groupChat?.listOfUsers?.map { it.userId } ?: emptyList()
    }



    private fun setupAdapter(){

        val isClientOnRight = currentUser.role == Constants.ROLE.CLIENT
        adapter = currentUser.userId?.let { MessagesAdapter(it, isClientOnRight, onMessageClickListener) }!!
        binding.rvChat.adapter = adapter
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
            /*reverseLayout = true*/  // Reverse layout for better pagination
        }
        adapter.updateLastVisitedTimestamp(lastTimeVisitedThisGroupTimestamp)
    }



    private fun setUpListener(){

        binding.includeTextSender.btnSendMsg.setOnClickListener(this)
        binding.includeTextSender.btnCamera.setOnClickListener(this)
        binding.includeTopBar.container.setOnClickListener(this)
        binding.includeTopBar.btnBack.setOnClickListener(this)
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
        chatViewModel.messages.observe(viewLifecycleOwner) { response ->

            val oldLastMessagePosition = adapter.itemCount - 1
            idOfLastMsgInGroup = response.lastOrNull()?.msgId
            adapter.submitList(response) {
                binding.rvChat.scrollToPosition(adapter.itemCount - 1)

                // Ensure previous last message updates (To update the chat bubble of previous message
                if (oldLastMessagePosition >= 0) {
                    adapter.notifyItemChanged(oldLastMessagePosition)
                }
            }
        }
    }


    override fun onPause() {
        super.onPause()
        groupChat?.let { gc->
            gc.listOfUsers?.let {listOfGroupMembers->
                idOfLastMsgInGroup?.let { idOfLastMsg ->
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
        // firestore call to update lastSeenMsgId
    }

    override fun onClick(v: View?) {
        when(v){

            binding.includeTextSender.btnCamera ->{
                val bottomSheet = UploadImageDocsBottomSheet.createBottomsheet(
                    uploadOnSelectListener, bottomSheetSelectables)
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

}
