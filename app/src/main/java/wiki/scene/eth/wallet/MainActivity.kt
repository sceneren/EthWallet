package wiki.scene.eth.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.consenlabs.tokencore.wallet.KeystoreStorage
import org.consenlabs.tokencore.wallet.WalletManager
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.util.EthWalletUtils
import java.io.File

class MainActivity : AppCompatActivity(), KeystoreStorage {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WalletManager.storage = this
        WalletManager.scanWallets()
        getCurrentWallet.setOnClickListener {
            EthWalletUtils.getWalletList()
                    .subscribe {
                        it.forEach { myWallet ->
                            Log.e("address", myWallet.wallet.address)
                        }
                    }
        }

        createWallet.setOnClickListener {
            createWallet()

        }
        createMnemonic.setOnClickListener {
            createMnemonic()
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun createWallet() {
        EthWalletUtils.createEthWallet(WalletType.ETH_WALLET_TYPE_SET, "xx", "12345678")
                .flatMap {
                    Log.e("xxx11", it.walletType.name)
                    Log.e("xxx22", it.wallet.address)
                    EthWalletUtils.getWalletMnemonic(it.wallet.id, "12345678")
                }
                .subscribe {
                    Log.e("xxx33", it.joinToString(" "))
                }

    }

    @SuppressLint("CheckResult")
    private fun createMnemonic() {

    }

    override fun getKeystoreDir(): File {
        return filesDir
    }
}