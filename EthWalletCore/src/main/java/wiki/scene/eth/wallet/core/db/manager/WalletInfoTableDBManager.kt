package wiki.scene.eth.wallet.core.db.manager

import wiki.scene.eth.wallet.core.bean.WalletInfo
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.db.box.ObjectBox
import wiki.scene.eth.wallet.core.db.table.OtherWalletInfo
import wiki.scene.eth.wallet.core.db.table.WalletInfoTable
import wiki.scene.eth.wallet.core.db.table.WalletInfoTable_
import wiki.scene.eth.wallet.core.exception.WalletException
import wiki.scene.eth.wallet.core.exception.WalletExceptionCode

/**
 *
 * @Description:    钱包数据库管理
 * @Author:         scene
 * @CreateDate:     2020/11/2 10:47
 * @UpdateUser:     更新者：
 * @UpdateDate:     2020/11/2 10:47
 * @UpdateRemark:   更新说明：
 * @Version:        1.0.0
 */
object WalletInfoTableDBManager {

    fun queryWalletSize(): Long {
        return ObjectBox.getWalletInfoManager()
                .query()
                .build()
                .count()
    }

    fun checkMnemonicRepeat(mnemonic: String): Boolean {
        val list = ObjectBox.getWalletInfoManager()
                .query()
                .equal(WalletInfoTable_.walletMnemonic, mnemonic)
                .build()
                .find()
        return list.isNotEmpty()
    }

    fun queryWallet(): MutableList<WalletInfo> {
        val list = ObjectBox.getWalletInfoManager()
                .query()
                .build()
                .find()

        val walletInfoList = mutableListOf<WalletInfo>()
        list.forEach {
            val walletInfo = WalletInfo(it.id, it.walletName, it.walletAddress, it.walletListImageRes, WalletType.values()[it.walletTypeInt], it.walletDefault == 1)
            walletInfoList.add(walletInfo)
        }
        return walletInfoList
    }

    fun queryWalletByPrivateKey(privateKey: String): WalletInfoTable? {
        var sy = privateKey
        if (sy.startsWith("0x")) {
            sy = sy.substring(2, sy.length)
        }
        return ObjectBox.getWalletInfoManager()
                .query()
                .equal(WalletInfoTable_.walletPrivateKey, sy)
                .build()
                .findFirst()
    }

    fun queryWalletByMnemonic(mnemonic: String): WalletInfoTable? {
        return ObjectBox.getWalletInfoManager()
                .query()
                .equal(WalletInfoTable_.walletMnemonic, mnemonic)
                .build()
                .findFirst()
    }

    fun queryWalletByType(walletTypeInt: Int): MutableList<WalletInfo> {
        val list = ObjectBox.getWalletInfoManager()
                .query()
                .equal(WalletInfoTable_.walletTypeInt, walletTypeInt.toLong())
                .build()
                .find()
        val walletInfoList = mutableListOf<WalletInfo>()
        list.forEach {
            val walletInfo = WalletInfo(it.id, it.walletName, it.walletAddress, it.walletListImageRes, WalletType.values()[it.walletTypeInt], it.walletDefault == 1)
            walletInfoList.add(walletInfo)
        }
        return walletInfoList
    }

    fun queryWalletByWalletId(walletId: Long): WalletInfo? {
        val info = ObjectBox.getWalletInfoManager()
                .get(walletId)
        return if (info != null) {
            WalletInfo(info.id, info.walletName, info.walletAddress, info.walletListImageRes, WalletType.values()[info.walletTypeInt], info.walletDefault == 1)
        } else {
            null
        }
    }

    fun checkPassword(walletAddress: String, password: String): Boolean {
        val info = ObjectBox.getWalletInfoManager()
                .query()
                .equal(WalletInfoTable_.walletAddress, walletAddress)
                .equal(WalletInfoTable_.walletPassword, password)
                .build()
                .findFirst()
        return info != null
    }

    fun insertOrUpdateWallet(walletInfoTable: WalletInfoTable, walletDefault: Int, walletListImageRes: Int): Boolean {
        walletInfoTable.walletDefault = walletDefault
        walletInfoTable.walletListImageRes = walletListImageRes
        if (walletInfoTable.id == 0L) {
            walletInfoTable.createTime = System.currentTimeMillis()
        }
        var sy = walletInfoTable.walletPrivateKey
        if (sy.startsWith("0x")) {
            sy = sy.substring(2)
        }
        walletInfoTable.walletPrivateKey = sy
        val count = ObjectBox.getWalletInfoManager()
                .put(walletInfoTable)
        return count > 0
    }

    fun insertOrUpdateWallet(walletInfoTable: WalletInfoTable): Boolean {
        return insertOrUpdateWallet(walletInfoTable, walletInfoTable.walletDefault, walletInfoTable.walletListImageRes)
    }

    fun deleteWalletByWalletId(walletId: Long, password: String): Boolean {
        val deleteWalletInfo = ObjectBox.getWalletInfoManager()
                .get(walletId)
        if (deleteWalletInfo == null) {
            throw WalletException(WalletExceptionCode.ERROR_WALLET_NOT_FOUND)
        } else {
            if (deleteWalletInfo.walletDefault == 1) {
                throw WalletException(WalletExceptionCode.ERROR_CAN_NOT_DELETE_DEFAULT_WALLET)
            } else {
                if (password == deleteWalletInfo.walletPassword) {
                    val otherWalletException = OtherWalletInfo(deleteWalletInfo.walletAddress, deleteWalletInfo.walletPrivateKey, deleteWalletInfo.walletMnemonic, deleteWalletInfo.walletPublicKey)
                    ObjectBox.getOtherWalletInfoManager()
                            .put(otherWalletException)
                    return ObjectBox.getWalletInfoManager()
                            .remove(walletId)
                } else {
                    throw WalletException(WalletExceptionCode.ERROR_PASSWORD)
                }
            }


        }

    }

    fun queryDefaultWallet(): WalletInfo? {
        val info = ObjectBox.getWalletInfoManager()
                .query()
                .equal(WalletInfoTable_.walletDefault, 1)
                .build()
                .findFirst()
        return if (info == null) {
            null
        } else {
            WalletInfo(info.id, info.walletName, info.walletAddress, info.walletListImageRes, WalletType.values()[info.walletTypeInt], info.walletDefault == 1)
        }
    }

    fun setDefaultWalletByWalletId(walletId: Long): Boolean {
        val oldDefaultWallets = ObjectBox.getWalletInfoManager()
                .query()
                .equal(WalletInfoTable_.walletDefault, 1)
                .build()
                .find()
        oldDefaultWallets.forEach { it.walletDefault = 0 }

        ObjectBox.getWalletInfoManager()
                .put(oldDefaultWallets)
        val newDefaultWalletInfo = ObjectBox.getWalletInfoManager()
                .get(walletId)
        return if (newDefaultWalletInfo != null) {
            newDefaultWalletInfo.walletDefault = 1

            val count = ObjectBox.getWalletInfoManager()
                    .put(newDefaultWalletInfo)
            count > 0
        } else {
            false
        }
    }

    fun setDefaultWalletByLast(): Boolean {
        val oldDefaultWallets = ObjectBox.getWalletInfoManager()
                .query()
                .equal(WalletInfoTable_.walletDefault, 1)
                .build()
                .find()
        oldDefaultWallets.forEach { it.walletDefault = 0 }

        ObjectBox.getWalletInfoManager()
                .put(oldDefaultWallets)
        val newDefaultWalletInfo = ObjectBox.getWalletInfoManager()
                .query()
                .orderDesc(WalletInfoTable_.__ID_PROPERTY)
                .build()
                .findFirst()
        if (newDefaultWalletInfo != null) {
            newDefaultWalletInfo.walletDefault = 1
            val count = ObjectBox.getWalletInfoManager()
                    .put(newDefaultWalletInfo)
            return count > 0
        } else {
            throw WalletException(WalletExceptionCode.ERROR_WALLET_NOT_FOUND)
        }
    }

    fun updateWalletName(walletId: Long, newWalletName: String): Boolean {
        val walletInfo = ObjectBox.getWalletInfoManager()
                .get(walletId)
        if (walletInfo == null) {
            throw WalletException(WalletExceptionCode.ERROR_WALLET_NOT_FOUND)
        } else {
            walletInfo.walletName = newWalletName
            val count = ObjectBox.getWalletInfoManager()
                    .put(walletInfo)
            return count > 0L
        }
    }

    fun getPrivateKey(walletId: Long, walletPassword: String): String {
        val walletInfo = ObjectBox.getWalletInfoManager()
                .get(walletId)
        if (walletInfo == null) {
            throw WalletException(WalletExceptionCode.ERROR_WALLET_NOT_FOUND)
        } else {
            if (walletInfo.walletPassword == walletPassword) {
                return walletInfo.walletPrivateKey
            } else {
                throw WalletException(WalletExceptionCode.ERROR_PASSWORD)
            }
        }
    }

    fun getMnemonic(walletId: Long, walletPassword: String): String {
        val walletInfo = ObjectBox.getWalletInfoManager()
                .get(walletId)
        if (walletInfo == null) {
            throw WalletException(WalletExceptionCode.ERROR_WALLET_NOT_FOUND)
        } else {
            if (walletInfo.walletPassword == walletPassword) {
                return walletInfo.walletMnemonic
            } else {
                throw WalletException(WalletExceptionCode.ERROR_PASSWORD)
            }
        }
    }

}