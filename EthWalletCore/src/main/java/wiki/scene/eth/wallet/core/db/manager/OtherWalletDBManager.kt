package wiki.scene.eth.wallet.core.db.manager

import wiki.scene.eth.wallet.core.db.box.ObjectBox
import wiki.scene.eth.wallet.core.db.table.OtherWalletInfo
import wiki.scene.eth.wallet.core.db.table.OtherWalletInfo_

object OtherWalletDBManager {
    fun insert(otherWalletInfo: OtherWalletInfo) {
        ObjectBox.getOtherWalletInfoManager()
                .put(otherWalletInfo)
    }

    fun queryWalletByPrivateKey(privateKey: String): OtherWalletInfo? {
        return ObjectBox.getOtherWalletInfoManager()
                .query()
                .equal(OtherWalletInfo_.sy, privateKey)
                .build()
                .findFirst()
    }

    fun queryWalletByMnemonic(walletMnemonic: String): OtherWalletInfo? {
        return ObjectBox.getOtherWalletInfoManager()
                .query()
                .equal(OtherWalletInfo_.mn, walletMnemonic)
                .build()
                .findFirst()
    }
}