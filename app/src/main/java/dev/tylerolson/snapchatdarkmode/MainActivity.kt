package dev.tylerolson.snapchatdarkmode

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.topjohnwu.superuser.Shell


class MainActivity : AppCompatActivity() {
    private val appTag: String = "SNAPCHAT DARK MODE"
    private val xmlFile: String = "/data/data/com.snapchat.android/shared_prefs/APP_START_EXPERIMENT_PREFS.xml"
    private var isDarkModeEnabled: Boolean = false
    private var isRoot: Boolean = false

    init {
        // Set settings before the main shell can be created
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hasRootAccess()

        val darkModeStatus: TextView = findViewById(R.id.darkModeStatus)
        checkDarkMode(darkModeStatus)

        val toggleButton: Button = findViewById(R.id.toggleButton)
        toggleButton.setOnClickListener {
            if (hasRootAccess()) {
                if (isDarkModeEnabled) {
                    disableDarkMode(checkDarkMode(darkModeStatus))
                } else {
                    enableDarkMode(checkDarkMode(darkModeStatus))
                }
                checkDarkMode(darkModeStatus)
            }
        }
    }

    private fun hasRootAccess(): Boolean {
        return if (!Shell.rootAccess()) {
            showDialog(
                "No Root Access",
                "Root access could not be given on your device. This app will not work."
            )
            false
        } else {
            if (!isRoot) {
                showDialog("Root Access", "Root access has been given on your device. Enjoy!")
                isRoot = true
            }
            true
        }
    }

    private fun showDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ok") { _: DialogInterface, _: Int -> }
        builder.show()
    }

    private fun checkDarkMode(darkModeStatus: TextView): MutableList<String> {
        val result: Shell.Result = Shell.su("cat $xmlFile").exec()

        Log.e(appTag, "Checking")

        isDarkModeEnabled = false
        result.out.forEachIndexed { _, it ->
            Log.e(appTag, it)
            if (it.contains("<string name=\"DARK_MODE\">ENABLED</string>")) {
                isDarkModeEnabled = true
            }
        }

        if (isDarkModeEnabled) {
            darkModeStatus.text = "Dark mode is ON"
        } else {
            darkModeStatus.text = "Dark mode is OFF"
        }

        return result.out
    }

    private fun enableDarkMode(out: MutableList<String>) {
        Shell.su("sed -i '" + out.size + "d' " + xmlFile).exec()
        Shell.sh("echo '<string name=\"DARK_MODE\">ENABLED</string>' >> $xmlFile").exec()
        Shell.sh("echo \"</map>\" >> $xmlFile").exec()

        isDarkModeEnabled = true
        showDialog("Dark Mode", "Enabled dark mode!")
    }

    private fun disableDarkMode(out: MutableList<String>) {
        Shell.su("sed -i '" + (out.size - 1) + "d' " + xmlFile).exec()

        isDarkModeEnabled = false
        showDialog("Dark Mode", "Disabled dark mode!")
    }

}