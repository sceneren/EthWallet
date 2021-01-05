package wiki.scene.eth.wallet

import androidx.multidex.MultiDexApplication
import com.orhanobut.hawk.Hawk
import com.tencent.mmkv.MMKV
import wiki.scene.eth.wallet.core.util.EthWalletCore


class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        EthWalletCore.init(this)
        MMKV.initialize(this)
        Hawk.init(this).build()
    }
}