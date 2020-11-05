package wiki.scene.eth.wallet.core.exception

/**
 *
 * @Description:    钱包异常
 * @Author:         scene
 * @CreateDate:     2020/10/23 10:56
 * @UpdateUser:     更新者：
 * @UpdateDate:     2020/10/23 10:56
 * @UpdateRemark:   更新说明：
 * @Version:        1.0.0
 */
object WalletExceptionCode {
    //未知错误
    const val ERROR_UNKNOWN = "8000"
    const val UNKNOWN = "unknown"

    //错误的助记词
    const val ERROR_MNEMONIC = "8001"

    //错误的密码
    const val ERROR_PASSWORD = "8002"
    const val WALLET_INVALID_PASSWORD = "password_incorrect"

    //密码为空
    const val PASSWORD_BLANK = "password_blank"

    //错误的私钥
    const val ERROR_PRIVATE_KEY = "8003"

    //未找到钱包
    const val ERROR_WALLET_NOT_FOUND = "8004"

    //钱包已存在
    const val ERROR_WALLET_EXITS = "8005"

    //密码太弱
    const val PASSWORD_WEAK = "password_weak"

    //助记词错误
    const val MNEMONIC_BAD_WORD = "mnemonic_word_invalid"
    const val MNEMONIC_INVALID_LENGTH = "mnemonic_length_invalid"
    const val MNEMONIC_CHECKSUM = "mnemonic_checksum_invalid"

    //未找到钱包
    const val WALLET_NOT_FOUND = "wallet_not_found"

    //无效的KeyStory
    const val WALLET_INVALID_KEYSTORE = "keystore_invalid"
    const val WALLET_INVALID = "keystore_invalid"

    //无效的钱包地址
    const val WALLET_INVALID_ADDRESS = "address_invalid"

    //无效的交易数据
    const val INVALID_TRANSACTION_DATA = "transaction_data_invalid"

    //地址已存在
    const val WALLET_EXISTS = "address_already_exist"

    //无效的Keystore
    const val INVALID_WALLET_VERSION = "keystore_version_invalid"

    //HD钱包不支持私有的
    const val WALLET_HD_NOT_SUPPORT_PRIVATE = "hd_not_support_private"

    //签名错误
    const val IPFS_CHECK_SIGNATURE = "check_signature"

    //私钥地址不匹配
    const val PRIVATE_KEY_ADDRESS_NOT_MATCH = "private_key_address_not_match"

    //无效的数量
    const val INVALID_BIG_NUMBER = "big_number_invalid"

    //无效的私钥
    const val PRIVATE_KEY_INVALID = "privatekey_invalid"

    const val INVALID_IDENTITY = "invalid_identity"

    //余额小于最小值
    const val AMOUNT_LESS_THAN_MINIMUM = "amount_less_than_minimum"

    //keystore包含无效的私钥
    const val KEYSTORE_CONTAINS_INVALID_PRIVATE_KEY = "keystore_contains_invalid_private_key"


}