package com.tvf.clb.base.model.zbet;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Deductions {
    @SerializedName("selection_id")
    private Long selectionsId;
}
