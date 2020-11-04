package wiki.scene.eth.wallet.core.manager

import wiki.scene.eth.wallet.core.db.box.ObjectBox
import wiki.scene.eth.wallet.core.db.table.OtherWalletInfo

object MyOtherWalletManager {
    fun insert(otherWalletInfo: OtherWalletInfo) {
        ObjectBox.getOtherWalletInfoManager()
                .put(otherWalletInfo)
    }
}