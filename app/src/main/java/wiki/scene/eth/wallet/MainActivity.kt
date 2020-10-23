package wiki.scene.eth.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.consenlabs.tokencore.wallet.Identity
import org.consenlabs.tokencore.wallet.KeystoreStorage
import org.consenlabs.tokencore.wallet.WalletManager
import org.consenlabs.tokencore.wallet.model.Metadata.P2WPKH
import org.consenlabs.tokencore.wallet.model.Network.MAINNET
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
        val identity = if (Identity.getCurrentIdentity() == null) {
            Identity.createIdentity("scene's Identity", "", "", MAINNET, P2WPKH)
        } else {
            Identity.currentIdentity
        }

        val ethWallet = identity.wallets[0]
        WalletManager.changePassword(ethWallet.id, "", "111111")
        val privateKey = WalletManager.exportPrivateKey(ethWallet.id, "111111")
        val asd = WalletManager.exportMnemonic(ethWallet.id, "111111")
        tvResult1.text = ethWallet.address
        Log.e("ethWallet.address", ethWallet.address + "\n" + privateKey + "\n" + asd)
    }

    @SuppressLint("CheckResult")
    private fun createMnemonic() {

    }

    override fun getKeystoreDir(): File {
        return filesDir
    }
}