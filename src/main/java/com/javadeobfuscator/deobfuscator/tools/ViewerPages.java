/*
 * Copyright 2026 GooseyPrime / java-deobfuscator
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.javadeobfuscator.deobfuscator.tools;

public final class ViewerPages {
    private final String leftTitle;
    private final String leftText;
    private final String rightTitle;
    private final String rightText;

    public ViewerPages(String leftTitle, String leftText, String rightTitle, String rightText) {
        this.leftTitle = leftTitle;
        this.leftText = leftText;
        this.rightTitle = rightTitle;
        this.rightText = rightText;
    }

    public String getLeftTitle() {
        return leftTitle;
    }

    public String getLeftText() {
        return leftText;
    }

    public String getRightTitle() {
        return rightTitle;
    }

    public String getRightText() {
        return rightText;
    }
}
