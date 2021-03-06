# tfs-virtual-file-system
A FAT-based file system (TFS) aimed at micro mobile devices. Includes an API and lightweight CLI (shell) for functional testing.

Below is a general overview of the system structure. Also check out the [project wiki](https://github.com/OCBier/virtual-disk-file-system/wiki) for more info.


## Packages Overview
***********

**`tfs`**: Contains the main subpackages of the different layers that make up the file system. Presents functionality to the shell through an API.

**`tfs.structures`**: Provides support for `tfs` through several different classes representing file system elements (structures). 

**`tfs.exceptions`**: Represents different exceptions which may occur in the file system.


<br />


## Testing Overview
************
Several driver programs to test the various modules and supporting classes of the file system are included in `tfs.testing`. 

