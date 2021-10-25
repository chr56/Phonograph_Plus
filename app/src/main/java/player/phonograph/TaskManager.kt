/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph

class TaskManager {
    private val taskList: MutableList<Task> = ArrayList()

    fun addTask(task: Task) { taskList.add(task) }
    fun removeTask() { taskList.removeLast() }
    fun removeTask(taskId: Int) {
        for (task in taskList) {
            if (task.getTaskId() == taskId)
                taskList.remove(task)
        }
    }

    fun executeTask(code: Int) {
        taskList.last().callback?.invoke(code)
    }
    fun executeTask(taskId: Int, code: Int) {
        for (task in taskList) {
            if (task.getTaskId() == taskId)
                task.callback?.invoke(code)
        }
    }

    /**
     *  execute first task with target action
     */
    fun executeTask(action: String, code: Int){
        for (task in taskList) {
            if (task.action.equals(action)) {
                task.callback?.invoke(code)
                return
            }
        }
    }
}

typealias Callback = (Int) -> Unit

class Task(private val taskId: Int, var callback: Callback?) {
    fun getTaskId(): Int = taskId

    var action: String? = null
    var data: String? = null // just in case
}
