/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto2so.kernel;

/**
 *
 * @author ani
 */


class LockEntry {
    final String resource;
    int sharedCount;
    int exclusiveOwnerPid; // -1 si no hay

    LockEntry(String resource) {
        this.resource = resource;
        this.sharedCount = 0;
        this.exclusiveOwnerPid = -1;
    }

    boolean isFree() {
        return sharedCount == 0 && exclusiveOwnerPid == -1;
    }
}