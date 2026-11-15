package om.sstvsuite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import om.sstvsuite.ui.SstvRoot
import ui.feice.mochi.utils.mochiEnableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        mochiEnableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { SstvRoot() }
    }
}
