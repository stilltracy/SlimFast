/*-
 *
 *  This file is part of Oracle Berkeley DB Java Edition
 *  Copyright (C) 2002, 2015 Oracle and/or its affiliates.  All rights reserved.
 *
 *  Oracle Berkeley DB Java Edition is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation, version 3.
 *
 *  Oracle Berkeley DB Java Edition is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License in
 *  the LICENSE file along with Oracle Berkeley DB Java Edition.  If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 *  An active Oracle commercial licensing agreement for this product
 *  supercedes this license.
 *
 *  For more information please contact:
 *
 *  Vice President Legal, Development
 *  Oracle America, Inc.
 *  5OP-10
 *  500 Oracle Parkway
 *  Redwood Shores, CA 94065
 *
 *  or
 *
 *  berkeleydb-info_us@oracle.com
 *
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  [This line intentionally left blank.]
 *  EOF
 *
 */

package persist.sqlapp;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;
import static com.sleepycat.persist.model.DeleteAction.NULLIFY;
import static com.sleepycat.persist.model.Relationship.MANY_TO_ONE;

/**
 * The Employee entity class.
 * 
 * @author chao
 */
@Entity
class Employee {

    @PrimaryKey
    int employeeId;

    /* Many Employees may have the same name. */
    @SecondaryKey(relate = MANY_TO_ONE)
    String employeeName;

    /* Many Employees may have the same salary. */
    @SecondaryKey(relate = MANY_TO_ONE)
    float salary;

    @SecondaryKey(relate = MANY_TO_ONE, relatedEntity=Employee.class,
                                        onRelatedEntityDelete=NULLIFY)
    Integer managerId; // Use "Integer" to allow null values.

    @SecondaryKey(relate = MANY_TO_ONE, relatedEntity=Department.class,
                                        onRelatedEntityDelete=NULLIFY)
    Integer departmentId;

    String address;

    public Employee(int employeeId,
                    String employeeName,
                    float salary,
                    Integer managerId,
                    int departmentId,
                    String address) {
        
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.salary = salary;
        this.managerId = managerId;
        this.departmentId = departmentId;
        this.address = address;
    }

    @SuppressWarnings("unused")
    private Employee() {} // Needed for deserialization.

    public String getAddress() {
        return address;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public float getSalary() {
        return salary;
    }

    @Override
    public String toString() {
        return this.employeeId + ", " +
               this.employeeName + ", " +
               this.salary + ", " +
               this.managerId + ", " +
               this.departmentId + ", " +
               this.address;
    }
}
