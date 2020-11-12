package wiki.scene.eth.wallet.core.util

import android.content.Context
import wiki.scene.eth.wallet.core.db.box.ObjectBox

object EthWalletCore {
    private var context: Context? = null
    private var walletFilePath: String? = null
    fun init(context: Context) {
        this.context = context
        ObjectBox.init(context)
        walletFilePath = "${context.filesDir}/MyWallet"
    }

    fun getContext(): Context {
        if (this.context == null) {
            throw Throwable("Please call EthWalletCore.init(context)")
        }
        return this.context!!
    }

    fun getWalletFilePath(): String {
        if (this.walletFilePath == null) {
            throw Throwable("Please call EthWalletCore.init(context)")
        }
        return this.walletFilePath!!
    }

}