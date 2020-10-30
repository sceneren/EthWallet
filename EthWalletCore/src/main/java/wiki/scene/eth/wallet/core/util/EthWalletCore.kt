package wiki.scene.eth.wallet.core.util

import android.content.Context
import org.consenlabs.tokencore.wallet.KeystoreStorage
import wiki.scene.eth.wallet.core.db.box.ObjectBox

object EthWalletCore {
    private var context: Context? = null
    private var keystoreStorage: KeystoreStorage? = null
    fun init(context: Context) {
        this.context = context
        ObjectBox.init(context)
        keystoreStorage = KeystoreStorage { context.filesDir }
        WalletManager.scanWallets()
    }

    fun getContext(): Context {
        if (this.context == null) {
            throw Throwable("Please call EthWalletCore.init(context)")
        }
        return this.context!!
    }

    fun getKeystoreStorage(): KeystoreStorage {
        if (this.keystoreStorage == null) {
            throw Throwable("Please call EthWalletCore.init(context)")
        }
        return this.keystoreStorage!!
    }

}