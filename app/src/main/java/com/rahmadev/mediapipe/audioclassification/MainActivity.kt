package com.rahmadev.mediapipe.audioclassification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mediapipe.tasks.components.containers.Classifications
import com.rahmadev.mediapipe.audioclassification.databinding.ActivityMainBinding
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var audioClassifierHelper: AudioClassifierHelper
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeAudioClassifierHelper()
        setListener()
        updateButtonStates()
        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        if (!allPermissionGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }
    }

    private fun allPermissionGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        val message = if (isGranted) "Permission granted" else "Permission denied"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setListener() {
        binding.btnStart.setOnClickListener {
            audioClassifierHelper.startAudioClassification()
            isRecording = true
            updateButtonStates()
        }

        binding.btnStop.setOnClickListener {
            audioClassifierHelper.stopAudioClassification()
            isRecording = false
            updateButtonStates()
        }
    }

    private fun updateButtonStates() {
        binding.apply {
            btnStart.isEnabled = !isRecording
            btnStop.isEnabled = isRecording
        }
    }

    private fun initializeAudioClassifierHelper() {
        audioClassifierHelper = AudioClassifierHelper(
            context = this,
            classifierListener = object : AudioClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                }

                override fun onResult(results: List<Classifications>, inferenceTime: Long) {
                    runOnUiThread {
                        results.let { result ->
                            if (result.isNotEmpty() && result[0].categories().isNotEmpty()) {
                                println(result)
                                val sortedCategories =
                                    result[0].categories().sortedByDescending { it?.score() }
                                val displayResult = sortedCategories.joinToString("\n") {
                                    "${it.categoryName()} " + NumberFormat.getPercentInstance()
                                        .format(it.score()).trim()
                                }
                                binding.tvResult.text = displayResult
                            } else {
                                binding.tvResult.text = ""
                            }
                        }
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (isRecording) {
            audioClassifierHelper.startAudioClassification()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::audioClassifierHelper.isInitialized) {
            audioClassifierHelper.stopAudioClassification()
        }
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.RECORD_AUDIO
    }
}