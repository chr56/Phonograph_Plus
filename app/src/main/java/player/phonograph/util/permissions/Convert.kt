/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.util.permissions


fun convertPermissionsResult(result: Map<String, Boolean>): List<Permission> =
    result.map { (id, granted) ->
        if (granted) GrantedPermission(id) else NonGrantedPermission(id)
    }