package org.tdf.sunflower.sync

import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.tdf.common.util.HexBytes

@RlpProps("stateRoot", "maxAccounts")
data class GetAccounts @RlpCreator constructor(val stateRoot: HexBytes, val maxAccounts: Int)