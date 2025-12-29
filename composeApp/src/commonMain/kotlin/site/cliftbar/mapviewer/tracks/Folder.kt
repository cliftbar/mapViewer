package site.cliftbar.mapviewer.tracks

import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val subFolders: List<Folder> = emptyList(),
    val trackIds: List<String> = emptyList()
)
