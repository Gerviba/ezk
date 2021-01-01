package hu.gerviba.ezk.backend.dto

import hu.schbme.ezk.model.NoteDetailsDto

data class GameMap(
        var sectors: MutableMap<String, List<NoteDetailsDto>>,
        var paths: MutableMap<String, List<NoteDetailsDto>>
)