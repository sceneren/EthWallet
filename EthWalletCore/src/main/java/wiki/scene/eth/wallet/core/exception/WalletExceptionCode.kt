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

    //错误的助记词
    const val ERROR_MNEMONIC = "8001"

    //错误的密码
    const val ERROR_PASSWORD = "8002"

    //错误的私钥
    const val ERROR_PRIVATE_KEY = "8003"

    //未找到钱包
    const val ERROR_WALLET_NOT_FOUND = "8004"

    //钱包已存在
    const val ERROR_WALLET_EXITS = "8005"

    //权限不存在
    const val ERROR_PERMISSION_DEFINE = "8006"

    //创建钱包文件失败
    const val ERROR_CREATE_WALLET_FILE_FAIL = "8007"

    //创建钱包失败
    const val ERROR_CREATE_WALLET_FAIL = "8008"

    //未找到默认的钱包
    const val ERROR_NOT_FIND_DEFAULT_WALLET = "8009"

    //未找到钱包
    const val ERROR_NOT_FIND_WALLET = "8010"

    //写入数据库失败
    const val ERROR_DATABASE = "8011"

}