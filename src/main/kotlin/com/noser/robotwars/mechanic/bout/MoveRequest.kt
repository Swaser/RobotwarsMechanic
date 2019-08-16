package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.tournament.Competitor

data class MoveRequest(val requestUuid: String,
                       val boutUuid: String,
                       val arena: Arena,
                       val playerCompetitorMap: Map<Int, Competitor>)