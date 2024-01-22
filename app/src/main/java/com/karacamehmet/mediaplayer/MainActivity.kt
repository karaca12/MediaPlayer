package com.karacamehmet.mediaplayer

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.postDelayed
import com.karacamehmet.mediaplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var audioMediaPlayer: MediaPlayer? = null
    private var videoMediaPlayer: MediaPlayer? = null
    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                when (getMediaType(it)) {
                    MediaType.AUDIO -> playAudioFile(it)
                    MediaType.VIDEO -> playVideoFile(it)
                }
            }
        }
    private var isUserSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Launching the audio picker
        binding.buttonAudio.setOnClickListener {
            getContent.launch("audio/*")
        }
        //Launching the video picker
        binding.buttonVideo.setOnClickListener {
            getContent.launch("video/*")
        }

        //For resuming and pausing of the audio player
        binding.imageButtonPauseResume.setOnClickListener {
            if (audioMediaPlayer != null) {
                if (audioMediaPlayer?.isPlaying!!) {
                    audioMediaPlayer?.pause()
                    binding.imageButtonPauseResume.setImageDrawable(getDrawable(R.drawable.baseline_play_circle_24))
                } else {
                    audioMediaPlayer?.start()
                    binding.imageButtonPauseResume.setImageDrawable(getDrawable(R.drawable.baseline_pause_circle_24))
                }

            }
        }

        //To stop and reset the audio player
        binding.imageButtonStop.setOnClickListener {
            if (audioMediaPlayer != null) {
                setVisibilityToNeutral()
                audioMediaPlayer?.reset()
            }
        }

        //For controlling the dragging and changing the progress on the seek bar
        binding.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioMediaPlayer?.seekTo(progress)
                }
                if (progress==seekBar?.max){
                    binding.imageButtonPauseResume.setImageDrawable(getDrawable(R.drawable.baseline_play_circle_24))
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
            }

        })
    }


    //For creating the audio media player according to the chosen file
    private fun playAudioFile(uri: Uri) {
        setVisibilityToAudio()
        audioMediaPlayer?.release()
        videoMediaPlayer?.release()
        audioMediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            prepare()
            start()
            binding.audioSeekBar.max = duration
            binding.textViewAudioTotalTime.text = formatTime(duration)
            Handler(Looper.getMainLooper()).postDelayed({
                updateSeekBar()
            }, 1000)
        }
    }

    //For creating the video media player according to the chosen file
    private fun playVideoFile(uri: Uri) {
        setVisibilityToVideo()
        videoMediaPlayer?.release()
        audioMediaPlayer?.reset()
        videoMediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            binding.videoView.setMediaController(android.widget.MediaController(this@MainActivity))
            binding.videoView.setVideoURI(uri)
            binding.videoView.requestFocus()
            start()
        }
    }

    //For obtaining the media type that the user picks
    private fun getMediaType(uri: Uri): MediaType {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, uri)

        val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
        val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
        return when {
            hasVideo != null && hasVideo == "yes" -> MediaType.VIDEO
            hasAudio != null && hasAudio == "yes" -> MediaType.AUDIO
            else -> throw Exception()
        }
    }
    enum class MediaType {
        AUDIO, VIDEO
    }

    //For updating the seekbar as the audio progresses or the user changes the progress
    private fun updateSeekBar() {
        if (!isUserSeeking) {
            val currentPosition = audioMediaPlayer?.currentPosition ?: 0
            binding.audioSeekBar.progress = currentPosition
            binding.textViewAudioCurrentTime.text = formatTime(currentPosition)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            updateSeekBar()
        }, 1000)
    }

    //For setting the visibility of the views according to what we are doing
    private fun setVisibilityToAudio() {
        binding.imageButtonPauseResume.setImageDrawable(getDrawable(R.drawable.baseline_pause_circle_24))
        binding.imageButtonPauseResume.visibility = View.VISIBLE
        binding.audioSeekBar.visibility = View.VISIBLE
        binding.textViewAudioCurrentTime.visibility = View.VISIBLE
        binding.textViewAudioTotalTime.visibility = View.VISIBLE
        binding.imageButtonStop.visibility = View.VISIBLE
        binding.videoView.visibility = View.INVISIBLE
    }
    private fun setVisibilityToVideo() {
        binding.imageButtonPauseResume.visibility = View.INVISIBLE
        binding.audioSeekBar.visibility = View.INVISIBLE
        binding.textViewAudioCurrentTime.visibility = View.INVISIBLE
        binding.textViewAudioTotalTime.visibility = View.INVISIBLE
        binding.imageButtonStop.visibility = View.INVISIBLE
        binding.videoView.visibility = View.VISIBLE
    }
    private fun setVisibilityToNeutral() {
        binding.imageButtonPauseResume.visibility = View.INVISIBLE
        binding.audioSeekBar.visibility = View.INVISIBLE
        binding.textViewAudioCurrentTime.visibility = View.INVISIBLE
        binding.textViewAudioTotalTime.visibility = View.INVISIBLE
        binding.imageButtonStop.visibility = View.INVISIBLE
        binding.videoView.visibility = View.INVISIBLE
    }

    //For formatting the current and total time to minute and seconds
    private fun formatTime(timeInMilliseconds: Int): String {
        val seconds = timeInMilliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioMediaPlayer?.release()
        videoMediaPlayer?.release()
    }

}