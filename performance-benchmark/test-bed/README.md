### Test Bed
These are the configurations used during bench-marking.

- Guest OS    : `Ubuntu 64-bit 16.04 VM`
- RAM         : `8 GB`
- CPU cores   : `4`

- Hypervisor  : `VMware Fusion Professional Version 8.1.1 (3771013)`
- Host OS     : `OS X EI Captain Version 10.11.5 (15F34) MacBook Pro (Mid 2015)`
- Processor   : `2.5 GHz Core i7`
- Memory      : `16 GB 1600 MHz DDR3`

### Configuration Changes in Ubuntu

- Maximum number of connections allowed has to be changed in this file.
  ```
  /etc/sysctl.conf 
  ```
- Increase open files limit in this file.  
  ```
  /etc/security/limits.conf
  ```
  
This directory contains those configuration files.