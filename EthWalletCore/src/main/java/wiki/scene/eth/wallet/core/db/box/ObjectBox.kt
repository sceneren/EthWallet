package wiki.scene.eth.wallet.core.db.box

import android.content.Context
import io.objectbox.Box
import io.objectbox.BoxStore
import wiki.scene.eth.wallet.core.db.table.MyObjectBox
import wiki.scene.eth.wallet.core.db.table.WalletInfoTable
import wiki.scene.eth.wallet.core.db.table.OtherWalletInfo
import wiki.scene.eth.wallet.core.db.table.WalletAddressInfo

object ObjectBox {
    lateinit var boxStore: BoxStore

    fun init(context: Context) {
        boxStore = MyObjectBox.builder().androidContext(context).build()
    }

    fun getWalletInfoManager(): Box<WalletInfoTable> {
        return boxStore.boxFor(WalletInfoTable::class.java)
    }

    fun getWalletAddressInfoManager(): Box<WalletAddressInfo> {
        return boxStore.boxFor(WalletAddressInfo::class.java)
    }

    fun getOtherWalletInfoManager(): Box<OtherWalletInfo> {
        return boxStore.boxFor(OtherWalletInfo::class.java)
    }
}