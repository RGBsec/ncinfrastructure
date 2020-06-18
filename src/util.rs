use std::error::Error;
use std::result::Result;

use log::Level;

use super::logger::*;

pub trait HandleError<T, E: Error> {
    fn handle(self, begin_error: &str, level: Level) -> T;
    fn handle_err(self, begin_error: &str) -> T;
    fn handle_warn(self, begin_error: &str) -> T;
}

impl<T, E: Error> HandleError<T, E> for Result<T, E> {
    fn handle(self, begin_error: &str, level: Level) -> T {
        match self {
            Err(e) => {
                log!(level, "{}: {}", begin_error, e);
                std::process::exit(-1);
            }
            Ok(v) => v,
        }
    }

    fn handle_err(self, begin_error: &str) -> T {
        self.handle(begin_error, Level::Error)
    }

    fn handle_warn(self, begin_error: &str) -> T {
        self.handle(begin_error, Level::Warn)
    }
}

pub trait HandleOption<T> {
    fn handle(self, begin_error: &str, level: Level) -> T;
    fn handle_err(self, begin_error: &str) -> T;
    fn handle_warn(self, begin_error: &str) -> T;
}

impl<T> HandleOption<T> for Option<T> {
    fn handle(self, begin_error: &str, level: Level) -> T {
        match self {
            None => {
                log!(level, "{} (unwrapped a None value)", begin_error);
                std::process::exit(-1);
            }
            Some(v) => v,
        }
    }

    fn handle_err(self, begin_error: &str) -> T {
        self.handle(begin_error, Level::Error)
    }

    fn handle_warn(self, begin_error: &str) -> T {
        self.handle(begin_error, Level::Warn)
    }
}
