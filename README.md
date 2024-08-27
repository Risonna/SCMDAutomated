# SCMDAutomated

![image](https://github.com/user-attachments/assets/47b29c1f-8d08-457c-bc28-33ad8762a7ce)


## Description

SCMDAutomated provides an easy and automated way to download Steam Workshop items via SteamCMD. This is a GUI-based application for steamcmd and it allows you to:

* Download individual Steam Workshop items.
* Download entire collections with a single click.
* Track download progress and status.
* View downloaded item locations.
* Re-attempt failed downloads.
* Download items that require login (with Steam Guard support).

## Getting Started

### Prerequisites

* **SteamCMD:** Make sure you have SteamCMD installed on your PC. For installation instructions, refer to the official [SteamCMD documentation](https://developer.valvesoftware.com/wiki/SteamCMD#Downloading_SteamCMD).

### Installation

1. Download the latest release of SCMDAutomated from **[here](https://github.com/Risonna/SCMDAutomated/releases/download/v.0.1/SCMDAutomated.zip)**. 
2. Extract the downloaded archive to a directory of your choice. It is recommended to use a path without spaces or special characters.
3. Run the `run.bat` file to start the application.

### Initial Setup

1. Upon launching the application for the first time, you will be prompted to navigate to the settings tab and specify the path to your SteamCMD executable. Click the **Settings** button to proceed. 
2. In the settings tab:
   * You can either manually paste the path to your SteamCMD executable into the designated field.
   * Or, click the **Browse** button to locate the executable file on your system.
3. After providing the SteamCMD path, click the **Save Settings** button. 
   * If the path is valid, the settings will be saved to a `steamcmd_path.txt` file in the same directory as the application, and after closing the settings window the main window will open.
   * If there are any issues with the provided path, you will receive an error message and need to re-enter the correct path.

## How to Use

1. **Enter Workshop Item URL:**
   * In the main window's input field at the top, paste a valid Steam Workshop item URL. This URL typically follows the format: `https://steamcommunity.com/sharedfiles/filedetails/?id=0` (replace '0' with the actual item ID).

2. **Item Information:**
   * If the entered URL is valid, the application will retrieve and display information about the item, including the game it belongs to.
   * If the item is already installed, a message will appear along with a button to open its location.

   ![image](https://github.com/user-attachments/assets/b2d084bb-39aa-469c-8104-14a5d94e8c5b)

3. **Download Item:**
   * Click the **Download** button next to the input field to initiate the download. 
   * A notification will appear in the top-right corner confirming the download initiation. The download will be added to the **Recent Downloads** list for tracking.

4. **Recent Downloads:**
   * Click the **Recent Downloads** button to open a window displaying the status of all recent downloads.

   ![image](https://github.com/user-attachments/assets/0b9e2b4c-4c15-44c4-9049-cfee9cece4df)
   ![image](https://github.com/user-attachments/assets/69846695-a23a-4ba3-b798-a4820587907e)

   * **Tracking Downloads:**  You can monitor the progress of each download in this window.
   * **Accessing Downloads:** Upon successful completion, a folder icon will appear next to the downloaded item. Clicking it will open the item's location on your system.

   ![image](https://github.com/user-attachments/assets/8975076c-71b2-4e62-80d0-2067b10db4fa)

   * **Retrying Downloads:** If a download fails, a reload icon will appear next to the item. Click this icon to retry the download.
   * **Clearing the List:** Click the **Clear All** button to clear the Recent Downloads list.
   * **Closing the Window:** Closing the Recent Downloads window will also dismiss the notification on the main window.

5. **Downloading Collections:**
   * You can download entire Workshop collections by entering the collection URL (similar format to individual item URLs).
   * **View Collection Items:** A **View Collection Items** button will appear. Clicking it opens a window listing all items in the collection.

   ![image](https://github.com/user-attachments/assets/a7f43dbd-ed95-49de-9231-a73f8b1c8741)
   ![image](https://github.com/user-attachments/assets/3354fcda-9c9a-4cc7-a6da-b801e87eab45)

   * **Individual Item Downloads:** You can download individual items from the collection list. Their download status is also tracked in the Recent Downloads window.
   * **Download All:** The main download button changes behaviour when a collection URL is entered. Clicking this button initiates the download of all items in the collection, adding them to the Recent Downloads list.
   * By opening the collection items window while having some items already installed on your system(not in recent downloads list), you will be able to open their location and to redownload them.
 ![image](https://github.com/user-attachments/assets/a5f63b82-1fd7-45ec-83a1-0baea12b9bf0)

6. **Using a Steam Account:**
   * Some items require you to be logged in to your Steam account to download (e.g., Civilization VI mods).
   * **Enable Login:** Check the **Use steam account** checkbox.

     ![image](https://github.com/user-attachments/assets/31313a94-1658-4254-b4e2-2b6a1fe6589d)

   * **Enter Credentials:** A login window will appear prompting you for your Steam username and password.

     ![image](https://github.com/user-attachments/assets/3258079a-ca3e-4387-b32d-24975e83af6c)

   * **Steam Guard:** If enabled, another window will appear and you'll be prompted to enter your Steam Guard code.
   * **Login Confirmation:** Successful login will close the login windows and check the **Use steam account** checkbox on the main window. To return to anonymous mode, uncheck the box. If it seems to be stuck for up to a minute, don't worry. The application needs to open a steamcmd instance and attempt to log in with your account data.

     **Security Note:**
     * Your Steam credentials are never stored permanently. They are only kept in encrypted format within the application's memory during a single session (from launch to closure).
     * For large collections, consider clearing your `userdata` and `steamapps` folders within the SteamCMD installation directory or logging into your Steam account before starting the download. Anonymous downloads utilize multiple SteamCMD instances for faster speeds, which can lead to a higher chance of failure compared to a single instance used during logged-in downloads.
     * 2FA isn't supported yet, so you will not be able to successfully login with it enabled.

## Footnote
This app was made as a fun side project so it may not work ideally in all cases, but it still gets work done most of the time.
