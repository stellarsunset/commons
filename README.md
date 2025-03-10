# commons

[![Test](https://github.com/stellarsunset/commons/actions/workflows/test.yaml/badge.svg)](https://github.com/stellarsunset/commons/actions/workflows/test.yaml)
[![codecov](https://codecov.io/github/stellarsunset/commons/graph/badge.svg?token=JIzptwIhbN)](https://codecov.io/github/stellarsunset/commons)

Common classes that may make it into the public API of my open-source repos.

## Usage

This repository provides a single, low-dependency place, for me to capture a small number of common APIs, e.g. container
classes, that multiple of my OSS projects use.

This saves me having to replicate them in multiple projects and prevents runtime conflicts for clients (and myself) when
using multiple of my OSS libraries in concert.

It's not expected that:

1. Any clients use this repository directly
2. This repo grow significantly in size, unless I get into data structures or something