package wiki.scene.eth.wallet

import androidx.multidex.MultiDexApplication
import wiki.scene.eth.wallet.core.util.EthWalletCore
import wiki.scene.eth.wallet.core.util.EthWalletUtils


class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        EthWalletCore.init(this)
    }
}