use ansi_term::Color;
use ansi_term::Style;
use log::Level;
pub use log::{debug, error, info, log, trace, warn};

fn level_to_ansi(level: &Level) -> String {
    let debug_style: ansi_term::Style = Style::new().on(Color::Fixed(237)).fg(Color::White);
    let error_style: ansi_term::Style = Style::new().on(Color::Red).fg(Color::Black);
    let info_style: ansi_term::Style = Style::new().on(Color::Blue).fg(Color::Black);
    let warn_style: ansi_term::Style = Style::new().on(Color::Yellow).fg(Color::Black);
    match level {
        Level::Debug => debug_style.paint(" DEBG ").to_string(),
        Level::Error => error_style.paint(" ERR! ").to_string(),
        Level::Info => info_style.paint(" INFO ").to_string(),
        Level::Trace => debug_style.paint(" DEBG ").to_string(),
        Level::Warn => warn_style.paint(" WARN ").to_string(),
    }
}

fn message_to_ansi(level: &Level, message: &str) -> String {
    let debug_format = Style::new().fg(Color::Fixed(247));
    let error_format = Style::new().fg(Color::Red);
    let warn_format = Style::new().fg(Color::Yellow);
    match level {
        Level::Debug => debug_format.paint(message).to_string(),
        Level::Error => error_format.paint(message).to_string(),
        Level::Info => message.to_string(),
        Level::Trace => debug_format.paint(message).to_string(),
        Level::Warn => warn_format.paint(message).to_string(),
    }
}

pub fn setup_logger() -> Result<(), fern::InitError> {
    let file_dispatch = fern::Dispatch::new()
        .format(|out, message, record| {
            out.finish(format_args!(
                "[{}] [{}] [{}]: {}",
                chrono::Local::now().format("%Y-%m-%d %H:%M:%S"),
                record.level(),
                record.target(),
                message
            ))
        })
        .level(log::LevelFilter::Debug)
        .chain(fern::log_file("output.log")?);

    let out_dispatch = fern::Dispatch::new()
        .format(|out, message, record| {
            out.finish(format_args!(
                "{} {} {}: {}",
                chrono::Local::now().format("%Y-%m-%d %H:%M:%S"),
                level_to_ansi(&record.level()),
                Style::new()
                    .on(Color::White)
                    .fg(Color::Black)
                    .paint(format!(" {} ", record.target()))
                    .to_string(),
                message_to_ansi(&record.level(), &message.to_string())
            ))
        })
        .level(log::LevelFilter::Debug)
        .chain(std::io::stdout());

    fern::Dispatch::new()
        .level(log::LevelFilter::Debug)
        .chain(out_dispatch)
        .chain(file_dispatch)
        .apply()?;
    Ok(())
}
