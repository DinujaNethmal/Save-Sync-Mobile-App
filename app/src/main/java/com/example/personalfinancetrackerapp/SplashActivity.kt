package com.example.personalfinancetrackerapp

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SplashActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView
    private lateinit var logo: ImageView
    private lateinit var text: TextView
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        try {
            videoView = findViewById(R.id.splashVideo)

            // Setup video
            setupVideo()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            // If there's any error, proceed to login screen
            navigateToNextScreen()
        }
    }

    private fun setupVideo() {
        try {
            // Set video path (you need to add your video to raw folder)
            val videoPath = "android.resource://" + packageName + "/" + R.raw.splash_video
            videoView.setVideoURI(Uri.parse(videoPath))

            // Set video completion listener
            videoView.setOnCompletionListener { mediaPlayer ->
                try {
                    mediaPlayer.release()
                    showLogoAndText()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in video completion: ${e.message}")
                    navigateToNextScreen()
                }
            }

            // Set error listener
            videoView.setOnErrorListener { mp, what, extra ->
                Log.e(TAG, "Video error: what=$what, extra=$extra")
                navigateToNextScreen()
                true
            }

            // Start video
            videoView.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up video: ${e.message}")
            navigateToNextScreen()
        }
    }

    private fun showLogoAndText() {
        try {
            // Show logo and text with animations
            logo.visibility = View.VISIBLE
            text.visibility = View.VISIBLE

            val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)

            logo.startAnimation(fadeIn)
            text.startAnimation(slideUp)

            // Handler to delay the next activity
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToNextScreen()
            }, 2000) // 2 seconds delay
        } catch (e: Exception) {
            Log.e(TAG, "Error showing logo and text: ${e.message}")
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        try {
            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

            val intent = if (isLoggedIn) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to next screen: ${e.message}")
            // If all else fails, try to go to login screen
            try {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Critical error: ${e.message}")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            videoView.stopPlayback()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}")
        }
    }
}