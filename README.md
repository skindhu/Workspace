# Workspace

---

The workspace is a wide area with a finite number of screens which we can scroll vertically or horizontally.
Each screen contains a number of icons, folders or widges the user can interact with.

##Usage:

```
<com.workspace.Workspace
 android:id="@+id/workspace" android:layout_width="fill_parent"
android:layout_height="100dp" android:background="@drawable/bg_texture"
workspace:orientation="vertical">
</com.workspace.Workspace>
```
with the custom attribute `workspace:orientation` , which value can be  `vertical` or `horizontal`, we can controll the scrolling direction .



