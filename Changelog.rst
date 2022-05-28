==================
ChangeLog for edge
==================

All notable changes to this project will be documented in this file.


Added
-----

- Add Oxford dictionaries API driver
- UI to allow install plugins from local jar or zip file.
- Add options to choose export TM folder and which TMs to export

Changed
-------

- Minimum requirements to Java 11
- Bump jGit@6.1.0 and use MINA-SSHD@2.7.0
- Bump Groovy@4.0.2
- Support online dictionary[#1597]
- Change way to update ``omegat.project`` file in git team project to allow mapping merge.
- Test: use ThreadLocalRandom for temporary filename
- Change branding to Edge edition
- AbstractFilter,PoFilter,MozillaLangFilter: Refactoring createWriter createReader

Fixed
-----

- Searcher: show file path in relative
- PoFilter handling of header comment [BUGS#1082]
- ExternalTMFactory: skip entry that is same as tuvSource
