package org.tdf.sunflower

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class Connector {


    @PostConstruct
    fun conn() {
//        cli.connectAsync(
//            "localhost",
//            30303,
//            "f6334e3488a58bc9c9ff9acefaef88255b511a6b68ea2bebccbc1f9e608317f378d7db42ddf2b053ec59f32d88c2409291bd61085caf553564d82db320e67170",
//            true
//        )
    }
}