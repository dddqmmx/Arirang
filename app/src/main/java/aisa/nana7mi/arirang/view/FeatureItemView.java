package aisa.nana7mi.arirang.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import aisa.nana7mi.arirang.R;

public class FeatureItemView extends FrameLayout {
    private TextView featureNameTextView;
    private String featureName;

    public FeatureItemView(Context context) {
        super(context);
        init(context);
    }

    public FeatureItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.FeatureItemView,
                    0, 0
            );
            try {
                featureName = a.getString(R.styleable.FeatureItemView_featureName);
                setFeatureName(featureName);
            } finally {
                a.recycle();
            }
        }
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.item_feature, this, true);
        featureNameTextView = findViewById(R.id.feature_name);
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
        featureNameTextView.setText(featureName);
    }

    public String getFeatureName() {
        return featureName;
    }
}
