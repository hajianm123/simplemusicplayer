package com.example.musicplayermamad
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.musicplayermamad.databinding.ActivityMainBinding

data class Song(val title: String, val artist: String, val path: String)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var songs: ArrayList<Song>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            setupSongList()
        }

        binding.fabPlay.setOnClickListener {
            mediaPlayer?.start()
        }

        binding.exit.setOnClickListener {
            finishAffinity()
            System.exit(0)
        }

        binding.fabPause.setOnClickListener {
            mediaPlayer?.pause()
        }

        binding.fabStop.setOnClickListener {
            stopMusic()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    private fun setupSongList() {
        songs = getAllAudio()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, songs.map { it.title })
        binding.songListView.adapter = adapter

        binding.songListView.setOnItemClickListener { _, _, position, _ ->
            val selectedSong = songs[position]
            showSongCover(selectedSong.path)
            playSong(selectedSong.path)
        }
    }

    private fun getAllAudio(): ArrayList<Song> {
        val songList = ArrayList<Song>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = contentResolver.query(uri, projection, selection, null, null)

        cursor?.use {
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val title = it.getString(titleCol)
                val artist = it.getString(artistCol)
                val path = it.getString(pathCol)
                songList.add(Song(title, artist, path))
            }
        }

        return songList
    }


    private fun playSong(path: String) {
        stopMusic()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(path)
            prepare()
            start()
        }
        initSeekbar()
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
//        mediaPlayer?.release()
//        mediaPlayer = null
        binding.seekBar.progress = 0
    }

    private fun initSeekbar() {
        binding.seekBar.max = mediaPlayer?.duration ?: 100
        val handler = Handler(mainLooper)
        handler.postDelayed(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    binding.seekBar.progress = it.currentPosition
                    handler.postDelayed(this, 1000)
                }
            }
        }, 0)
    }
    private fun showSongCover(songPath: String) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(songPath)
            val art = retriever.embeddedPicture
            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                binding.imageView.setImageBitmap(bitmap)
            } else {
                binding.imageView.setImageResource(R.drawable.photo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.imageView.setImageResource(R.drawable.photo)
        } finally {
            retriever.release()
        }
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setupSongList()
        } else {
            Toast.makeText(this, "مجوز دسترسی به فایل‌های صوتی لازم است", Toast.LENGTH_SHORT).show()
        }
    }
}
