# Backdrop
A Material Design Backdrop component (see https://material.io/design/components/backdrop.html)

A Backdrop is a custom ViewGroup that requires exactly two children, one 'back' and one 'front' (using the layout_layer attribute as shown in the example below).

Activate a layer with:
```kotlin
backdrop.activateLayer(Backdrop.Layer.FRONT)
```

Listen to layer activations with:
(e.g. change a toolbar's navigation icon to a close icon when the back layer is active and the menu icon when the front layer is active)
```kotlin
backdrop.addOnActivateLayerListener{
  when (it){
    Backdrop.Layer.BACK -> {/* Do something */}
    Backdrop.Layer.FRONT -> {/* Do something */}
  }
}
```

When the back layer is activated the front layer collapses, revealing the back layer. The front layer will only collapse so as to reveal the entire back layer, and won't collapse to a height smaller than that of the min_front_height attribute of the backdrop.

When the front layer is activated it expands. A section of the back layer will always remain visible. The height of this persistently revealed section can be set with either the persistent_back_height or persistent_back_view attributes of the backdrop.

The front layer is active by default.

The FrontLayer view is included for simplicity. It uses a rectangle with rounded top corners as the background, inflates the layout_front_subheader layout providing an expand button which is only shown when the front layer is collapsed and activates itself when tapped. Note that any view/viewgroup may be used as the front layer, so long as layout_front equals 'front'.

## Example

```xml

<com.hat.backdrop2.Backdrop android:id="@+id/backdrop"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/colorPrimary"
        app:persistent_back_view="@+id/toolbar">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            android:orientation="vertical"
            app:layout_layer="back">

            <android.support.v7.widget.Toolbar android:id="@+id/toolbar"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:titleTextColor="#ffffff"
                app:navigationIcon="@drawable/ic_menu_black_24dp"
                android:gravity="center_vertical"
                app:title="Harry Gatward"/>

            <android.support.design.chip.ChipGroup
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:paddingStart="16dp"
                android:paddingEnd="16dp">

                <android.support.design.chip.Chip
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    style="@style/Widget.MaterialComponents.Chip.Filter"
                    android:text="Android" />

                <android.support.design.chip.Chip
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    style="@style/Widget.MaterialComponents.Chip.Filter"
                    android:text="iOS" />

                <android.support.design.chip.Chip
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    style="@style/Widget.MaterialComponents.Chip.Filter"
                    android:text="Unity" />
            </android.support.design.chip.ChipGroup>
        </LinearLayout>

        <com.hat.backdrop2.FrontLayer
            android:layout_width="match_parent"
            android:layout_height="0dp"
            app:layout_layer="front"
            app:subheader="Subheader">

            <TextView android:id="@+id/content"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Content" />
        </com.hat.backdrop2.FrontLayer>
    </com.hat.backdrop2.Backdrop>

```


| attribute               | format    | explanation
|-------------------------|-----------|--------------------------------------------------------------------------------------|
| persistent_back_height  | dimension | the height of the back layer that remains visible after the front layer is activated |
| persistent_back_view    | reference | the view's height used to set the persistently visible section of the back layer     |
| min_front_height        | dimension | the minimum height of the front view                                                 |

| layout parameter  | format               |
|-------------------|----------------------|
| layout_layer      | enum (back or front) |
