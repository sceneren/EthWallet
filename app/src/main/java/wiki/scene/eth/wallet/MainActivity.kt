package wiki.scene.eth.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.db.table.WalletAddressInfo
import wiki.scene.eth.wallet.core.manager.WalletAddressManager
import wiki.scene.eth.wallet.core.util.EthWalletUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvResult1.text = System.currentTimeMillis().toString()

        btnCheckHasWallet.setOnClickListener {
            Log.e("开始时间：", System.currentTimeMillis().toString())
            EthWalletUtils.hasWallet().subscribe({
                Log.e("结束时间：", System.currentTimeMillis().toString())
                Log.e("是否有钱包", it.toString())
            }, { Log.e("错误", it.message!!) })
        }

        getCurrentWallet.setOnClickListener {
            EthWalletUtils.getDefaultWallet()
                    .subscribe({
                        Log.e("钱包数据", it.toString())
                        Log.e("address", it.toString())
                    }, {
                        Log.e("错误", it.message!!)
                    })
        }

        createWallet.setOnClickListener {

            EthWalletUtils.createMnemonic()
                    .flatMap {
                        return@flatMap EthWalletUtils.createEthWallet(WalletType.ETH_WALLET_TYPE_ETH, it, "ETH", "123", 0)
                    }.flatMap { return@flatMap EthWalletUtils.getWalletList() }
                    .subscribe {
                        Log.e("钱包数据", it.size.toString())
                        Log.e("address", it.toString())
                    }
        }
        createMnemonic.setOnClickListener {
            EthWalletUtils.getWalletListByType(WalletType.ETH_WALLET_TYPE_ETH)
                    .subscribe {
                        it.forEach { wallet -> Log.e("wallet", wallet.toString()) }
                    }
        }

        btnImportWalletByPrivateKey.setOnClickListener {
            EthWalletUtils.importWalletByPrivateKey(WalletType.ETH_WALLET_TYPE_ETH, "123126734", "xxx", "11112222", 0)
                    .subscribe {
                        Log.e("xx", it.walletName)
                    }
        }


        btnInsetAddress.setOnClickListener {
            WalletAddressManager.addOrUpdateWalletAddress(WalletAddressInfo(0, WalletType.ETH_WALLET_TYPE_ETH.ordinal, "0x1231213", "备注"))
                    .subscribe {
                        Log.e("xx", it.toString())
                    }
        }
        btnDeleteAddress.setOnClickListener {
            WalletAddressManager.deleteWalletAddressById(1L)
                    .subscribe({
                        Log.e("xx", it.toString())
                    }, {
                        Log.e("xx", it.message!!)
                    })
        }

        btnQueryAddress.setOnClickListener {
            WalletAddressManager.queryWalletAddress()
                    .subscribe({
                        Log.e("xx", it.toString())
                    }, {
                        Log.e("xx", it.message!!)
                    })
        }

        btnUpdateAddress.setOnClickListener {
            val addressInfo = WalletAddressInfo(0, 0, "xxx", "xxxxxxx")
            addressInfo.addressId = 1
            WalletAddressManager.addOrUpdateWalletAddress(addressInfo)
        }


    }

    @SuppressLint("CheckResult")
    private fun createMnemonic() {

    }

}