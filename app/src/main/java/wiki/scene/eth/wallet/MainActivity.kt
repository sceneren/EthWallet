package wiki.scene.eth.wallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import wiki.scene.eth.wallet.core.config.WalletType
import wiki.scene.eth.wallet.core.manager.WalletAddressManager
import wiki.scene.eth.wallet.core.db.table.WalletAddressInfo
import wiki.scene.eth.wallet.core.util.EthWalletUtils

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getCurrentWallet.setOnClickListener {
            EthWalletUtils.getWalletList()
                    .subscribe {
                        Log.e("钱包数据", it.size.toString())
                        Log.e("address", it.toString())
                    }
        }

        createWallet.setOnClickListener {
            createWallet()
        }
        createMnemonic.setOnClickListener {
            EthWalletUtils.getWalletListByType(WalletType.ETH_WALLET_TYPE_ETH)
                    .subscribe {
//                        it[0].wallet.setAccountName()
                        it.forEach { wallet -> Log.e("wallet", wallet.toString()) }
                    }
        }

        btnImportWalletByPrivateKey.setOnClickListener {
            EthWalletUtils.importWalletByPrivateKey(WalletType.ETH_WALLET_TYPE_ETH, "123126734", "xxx", "11112222")
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

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun createWallet() {
        EthWalletUtils.createEthWallet(WalletType.ETH_WALLET_TYPE_ETH, "xx", "12345678")
                .subscribe {
                    Log.e("xxx33", it.toString())
                }

    }

    @SuppressLint("CheckResult")
    private fun createMnemonic() {

    }

}