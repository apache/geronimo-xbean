/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.xbean.recipe;

/**
 * @version $Rev$ $Date$
 */
public class Car {
    public String make;
    public String model;
    public int year;

    public Car(String make, String model, int year) {
        if (make == null) throw new NullPointerException("make is null");
        if (model == null) throw new NullPointerException("model is null");

        this.make = make;
        this.model = model;
        this.year = year;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public String toString() {
        return "[Car: make=\"" + make + "\", model=\"" + model + "\", year=\"" + year + "\"]";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Car car = (Car) o;

        return year == car.year &&
                make.equals(car.make) &&
                model.equals(car.model);
    }

    public int hashCode() {
        int result;
        result = make.hashCode();
        result = 31 * result + model.hashCode();
        result = 31 * result + year;
        return result;
    }
}
