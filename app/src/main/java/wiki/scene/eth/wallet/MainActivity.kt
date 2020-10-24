package wiki.scene.eth.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.consenlabs.tokencore.wallet.KeystoreStorage
import org.consenlabs.tokencore.wallet.WalletManager
import java.io.File

class MainActivity : AppCompatActivity(), KeystoreStorage {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WalletManager.storage = this
        WalletManager.scanWallets()

        createWallet.setOnClickListener {
            createWallet()
        }
        createMnemonic.setOnClickListener {
            createMnemonic()
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun createWallet() {

    }

    @SuppressLint("CheckResult")
    private fun createMnemonic() {

    }

    override fun getKeystoreDir(): File {
        return filesDir
    }
}