package wiki.scene.eth.wallet.core.db.manager

import io.reactivex.Observable
import wiki.scene.eth.wallet.core.db.box.ObjectBox
import wiki.scene.eth.wallet.core.db.table.WalletAddressInfo
import wiki.scene.eth.wallet.core.db.table.WalletAddressInfo_
import wiki.scene.eth.wallet.core.ext.changeIOThread

/**
 *
 * @Description:    地址管理
 * @Author:         scene
 * @CreateDate:     2020/10/29 16:58
 * @UpdateUser:     更新者：
 * @UpdateDate:     2020/10/29 16:58
 * @UpdateRemark:   更新说明：
 * @Version:        1.0.0
 */
object WalletAddressDBManager {
    /**
     * 添加一条钱包地址
     * 如果存在就是修改
     */
    fun addOrUpdateWalletAddress(walletAddressInfo: WalletAddressInfo): Observable<Boolean> {
        walletAddressInfo.createTime = System.currentTimeMillis()
        return Observable.just(ObjectBox.getWalletAddressInfoManager()
                .put(walletAddressInfo) > 0)
                .changeIOThread()
    }

    /**
     * 根据Id来查询钱包对象
     */
    fun queryWalletAddressById(addressId: Long): Observable<WalletAddressInfo?> {
        return Observable.just(ObjectBox.getWalletAddressInfoManager()
                .query()
                .equal(WalletAddressInfo_.addressId, addressId)
                .build()
                .findFirst())
                .changeIOThread()
    }

    /**
     * 查询全部钱包
     */
    fun queryWalletAddress(): Observable<MutableList<WalletAddressInfo>> {
        return Observable.just(ObjectBox.getWalletAddressInfoManager()
                .query()
                .build()
                .find())
                .changeIOThread()
    }

    /**
     * 根据Id删除钱包
     */
    fun deleteWalletAddressById(addressId: Long): Observable<Boolean> {
        return Observable.just(ObjectBox.getWalletAddressInfoManager()
                .query()
                .equal(WalletAddressInfo_.addressId, addressId)
                .build()
                .remove() > 0)
                .changeIOThread()

    }
}