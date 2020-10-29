package wiki.scene.eth.wallet.core.manager

import io.reactivex.Observable
import wiki.scene.eth.wallet.core.ext.changeIOThread
import wiki.scene.eth.wallet.core.room.WalletDatabaseHelper
import wiki.scene.eth.wallet.core.room.table.WalletAddressInfo

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
object WalletAddressManager {
    /**
     * 添加一条钱包地址
     * 如果存在就是修改
     */
    fun addOrUpdateWalletAddress(walletAddressInfo: WalletAddressInfo): Observable<Boolean> {
        return Observable.just(WalletDatabaseHelper.getInstance()
                .walletAddressDao()
                .addOrUpdateWalletAddress(walletAddressInfo))
                .flatMap { Observable.just(true) }
                .changeIOThread()
    }

    /**
     * 根据Id来查询钱包对象
     */
    fun queryWalletAddressById(addressId: Int): Observable<WalletAddressInfo> {
        return Observable.just(WalletDatabaseHelper.getInstance()
                .walletAddressDao()
                .queryWalletAddressById(addressId))
                .flatMap {
                    if (it.size == 1) {
                        return@flatMap Observable.just(it[0])
                    } else {
                        return@flatMap Observable.error(Exception("data error"))
                    }
                }
                .changeIOThread()
    }

    /**
     * 查询全部钱包
     */
    fun queryWalletAddress(): Observable<MutableList<WalletAddressInfo>> {
        return Observable.just(WalletDatabaseHelper.getInstance()
                .walletAddressDao().queryAllWalletAddress())
                .changeIOThread()
    }

    /**
     * 根据Id删除钱包
     */
    fun deleteWalletAddressById(addressId: Int): Observable<Boolean> {
        return Observable.just(WalletDatabaseHelper.getInstance()
                .walletAddressDao()
                .deleteWalletAddressById(addressId))
                .flatMap { Observable.just(true) }
                .changeIOThread()

    }
}