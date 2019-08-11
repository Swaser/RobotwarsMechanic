package com.noser.robotwars.mechanic.bout

import com.noser.robotwars.mechanic.tournament.Competitor

data class MoveRequest(val requestId: String,
                       val boutUuid: String,
                       val arena: Arena,
                       val playersToCompetitors: Map<Int, Competitor>)