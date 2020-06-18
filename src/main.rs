#[macro_use]
extern crate clap;

pub mod logger;
pub mod util;

use logger::*;
use util::*;

use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpListener;

use std::env;
use std::error::Error;

use tokio::process::Command;

use std::process::Stdio;

use std::sync::Arc;
use tokio::sync::Mutex;

async fn read_write<R: AsyncReadExt + Unpin, W: AsyncWriteExt + Unpin>(
    read: &mut R,
    write: &Arc<Mutex<W>>,
) {
    let mut buf = [0u8; 1024];
    let n = match read.read(&mut buf).await {
        Err(e) => {
            warn!("Error while reading from socket: {}", e);
            None
        }
        Ok(amt) => Some(amt),
    };
    if let Some(n) = n {
        let mut write_guard = write.lock().await;
        if n == 0 {
            return;
        }
        if let Err(e) = write_guard.write_all(&buf[..]).await {
            warn!("Error while writing to process: {}", e)
        }
    }
}

async fn read_write_noguard<R: AsyncReadExt + Unpin, W: AsyncWriteExt + Unpin>(
    read: &mut R,
    write: &mut W,
) {
    let mut buf = [0u8; 1024];
    let n = match read.read(&mut buf).await {
        Err(e) => {
            warn!("Error while reading from socket: {}", e);
            None
        }
        Ok(amt) => Some(amt),
    };
    if let Some(n) = n {
        if n == 0 {
            return;
        }
        if let Err(e) = write.write_all(&buf[..]).await {
            warn!("Error while writing to process: {}", e)
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    setup_logger().expect("Failed to set up logger");

    let matches = clap_app!(ncinfrastructure =>
        (version: crate_version!())
        (author: "RGBsec <rgbsec@rgbsec.xyz>")
        (about: "Infrastructure for hosting pwn challenges over TCP.")
        (@arg BINARY: +required "The binary to run (as either a relative/absolute path or executable to resolve via PATH)")
        (@arg BIND: +required "The address and port to bind to (e.g. 127.0.0.1:8080)")
    ).get_matches();

    let binary = matches.value_of("BINARY").unwrap().to_string();
    let addr = matches.value_of("BIND").unwrap();

    let mut listener = TcpListener::bind(&addr).await?;
    info!("Listening on {}", addr);

    loop {
        let (socket, _) = listener.accept().await?;
        let binary_name = binary.clone();
        tokio::spawn(async move {
            let (mut read, mut write) = socket.into_split();
            let mut command = Command::new("/bin/bash");
            let mut command = command.arg("-c");
            command = command.arg(binary_name);
            command.stdin(Stdio::piped());
            command.stdout(Stdio::piped());
            command.stderr(Stdio::piped());
            let child = match command.spawn() {
                Ok(cmd) => Some(cmd),
                Err(e) => {
                    warn!("Error while spawning child process: {}", e);
                    None
                }
            };

            if child.is_none() {
                let err_string =
                    "Failed to spawn child process, please try again or contact administrators."
                        .as_bytes();
                if let Err(e) = write.write_all(err_string).await {
                    warn!("Error while writing to socket: {}", e);
                };
            }
            let mut child = child.unwrap();

            let mut stdout = child
                .stdout
                .take()
                .handle_err("Child did not have a handle to stdout");
            let mut stdin = child
                .stdin
                .take()
                .handle_err("Child did not have a handle to stdin");
            let mut stderr = child
                .stderr
                .take()
                .handle_err("Child did not have a handle to stderr");

            let write_mutex = Arc::new(Mutex::new(write));

            let write_stdout = write_mutex.clone();
            tokio::spawn(async move {
                loop {
                    read_write(&mut stdout, &write_stdout).await;
                }
            });

            let write_stderr = write_mutex.clone();
            tokio::spawn(async move {
                loop {
                    read_write(&mut stderr, &write_stderr).await;
                }
            });

            tokio::spawn(async move {
                loop {
                    read_write_noguard(&mut read, &mut stdin).await;
                }
            });

            if let Err(e) = child.await {
                warn!("Error while running process: {}", e)
            }
        });
    }
}
