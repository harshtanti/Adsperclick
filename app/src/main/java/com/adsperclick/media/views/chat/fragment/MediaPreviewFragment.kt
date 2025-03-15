package com.adsperclick.media.views.chat.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adsperclick.media.R
import com.adsperclick.media.databinding.FragmentMediaPreviewBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible
import com.adsperclick.media.views.chat.viewmodel.MediaPreviewViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.Player


@AndroidEntryPoint
class MediaPreviewFragment : Fragment() {

    lateinit var binding: FragmentMediaPreviewBinding

    private val viewModel: MediaPreviewViewModel by viewModels()

    private var mediaUrl: String? = null
    private var mediaType: Int? = null
    private var fileName: String? = null

    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            mediaUrl = it.getString("mediaUrl")
            mediaType = it.getString("mediaType")?.toInt()
            fileName = it.getString("fileName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupMediaPreview()
        setupObservers()

        // Set up download button
        binding.btnDownload.setOnClickListener {
            mediaUrl?.let { url ->
                viewModel.downloadMedia(url, fileName ?: "download")
            }
        }

        // Set up share button
        binding.btnShare.setOnClickListener {
            shareMedia()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = fileName ?: "Media Preview"
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupMediaPreview() {
        when (mediaType) {
            Constants.MSG_TYPE.IMG_URL -> {
                // Show image view, hide others
                binding.imagePreview.visible()
                binding.videoPlayerView.gone()
                binding.documentPreview.gone()

                // Load image using Glide
                Glide.with(requireContext())
                    .load(mediaUrl)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.logout_red)
                    .into(binding.imagePreview)

                binding.progressBar.gone()
            }

            Constants.MSG_TYPE.VIDEO -> {
                // Show video view, hide others
                binding.imagePreview.gone()
                binding.videoPlayerView.visible()
                binding.documentPreview.gone()

                // Video will be initialized in onStart()
                binding.progressBar.visible()
            }

            Constants.MSG_TYPE.DOCUMENT -> {
                // Show document view, hide others
                binding.imagePreview.gone()
                binding.videoPlayerView.gone()
                binding.documentPreview.visible()

                // Setup document preview UI
                binding.documentIcon.setImageResource(getDocumentIconResource(fileName ?: ""))
                binding.documentName.text = fileName ?: "Document"
                binding.documentInfo.text = "Tap to open document"

                // Set click listener to open document with system app
                binding.documentPreview.setOnClickListener {
                    openDocumentWithSystemApp()
                }

                binding.progressBar.gone()
            }

            else -> {
                // Unknown type - show error
                Toast.makeText(context, "Unsupported media type", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun setupObservers() {
        viewModel.downloadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MediaPreviewViewModel.DownloadState.Downloading -> {
                    binding.progressBar.visible()
                    binding.progressBar.progress = state.progress
                }
                is MediaPreviewViewModel.DownloadState.Success -> {
                    binding.progressBar.gone()
                    Toast.makeText(context, "Download complete", Toast.LENGTH_SHORT).show()

                    // Notify system about the new file
                    state.file?.let { file ->
                        notifySystemAboutNewFile(file)
                    }
                }
                is MediaPreviewViewModel.DownloadState.Error -> {
                    binding.progressBar.gone()
                    Toast.makeText(context, "Download failed: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.gone()
                }
            }
        }
    }

    private fun initializePlayer() {
        player = SimpleExoPlayer.Builder(requireContext()).build()
        binding.videoPlayerView.player = player

        mediaUrl?.let { url ->
            val mediaItem = MediaItem.fromUri(url)
            player?.apply {
                setMediaItem(mediaItem)
                playWhenReady = this@MediaPreviewFragment.playWhenReady
                seekTo(currentWindow, playbackPosition)
                addListener(playbackStateListener)
                prepare()
            }
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentWindow = exoPlayer.currentWindowIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()
        }
        player = null
    }

    private val playbackStateListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                binding.progressBar.gone()
            }
        }
    }

    private fun openDocumentWithSystemApp() {
        viewModel.getDownloadedFile(mediaUrl, fileName ?: "document")?.let { file ->
            openFile(file)
        } ?: run {
            // File not downloaded yet, start download
            mediaUrl?.let { url ->
                viewModel.downloadMedia(url, fileName ?: "document").observe(viewLifecycleOwner) { file ->
                    file?.let { openFile(it) }
                }
            }
        }
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(file.name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    context,
                    "No app found to open this type of file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareMedia() {
        viewModel.getDownloadedFile(mediaUrl, fileName ?: "media")?.let { file ->
            try {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = getMimeType(file.name)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, "Share via"))
            } catch (e: Exception) {
                Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            // File not downloaded yet, start download
            Toast.makeText(context, "Downloading file for sharing...", Toast.LENGTH_SHORT).show()
            mediaUrl?.let { url ->
                viewModel.downloadMedia(url, fileName ?: "media")
            }
        }
    }

    private fun notifySystemAboutNewFile(file: File) {
        val uri = Uri.fromFile(file)
        requireActivity().sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
    }

    private fun getMimeType(fileName: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private fun getDocumentIconResource(fileName: String): Int {
        return when {
            fileName.endsWith(".pdf", ignoreCase = true) -> R.drawable.ic_pdf
            fileName.endsWith(".doc", ignoreCase = true) ||
                    fileName.endsWith(".docx", ignoreCase = true) -> R.drawable.ic_word
            fileName.endsWith(".xls", ignoreCase = true) ||
                    fileName.endsWith(".xlsx", ignoreCase = true) -> R.drawable.ic_excel
            fileName.endsWith(".ppt", ignoreCase = true) ||
                    fileName.endsWith(".pptx", ignoreCase = true) -> R.drawable.ic_powerpoint
            fileName.endsWith(".txt", ignoreCase = true) -> R.drawable.ic_text
            else -> R.drawable.ic_document
        }
    }

    override fun onStart() {
        super.onStart()
        if (mediaType == Constants.MSG_TYPE.VIDEO) {
            if (Build.VERSION.SDK_INT >= 24) {
                initializePlayer()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mediaType == Constants.MSG_TYPE.VIDEO) {
            if (Build.VERSION.SDK_INT < 24 || player == null) {
                initializePlayer()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mediaType == Constants.MSG_TYPE.VIDEO) {
            if (Build.VERSION.SDK_INT  < 24) {
                releasePlayer()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (mediaType == Constants.MSG_TYPE.VIDEO) {
            if (Build.VERSION.SDK_INT  >= 24) {
                releasePlayer()
            }
        }
    }
}

