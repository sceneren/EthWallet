package wiki.scene.eth.wallet.core.util

import android.content.Context

object EthWalletCore {
    private var context: Context? = null
    fun init(context: Context) {
        this.context = context
    }

    fun getContext(): Context {
        if (this.context == null) {
            throw Throwable("Please call EthWalletCore.init(context)")
        }
        return this.context!!
    }

}