require 'gollum/app'
require 'digest/sha1'
require 'logger'

class App < Precious::App
  User = Struct.new(:name, :email, :password_hash, :can_write)
  LOGGER = Logger.new("/home/wiki/wiki_log.txt") 
  before { authenticate! }
  before /edit/ do   authorize_write ; end
  before do
        if @user
        session['gollum.author'] = {
          :name => @user.name,
          :email => @user.email
        }
        end
  end

  helpers do
    def public_path?(path)
        if path == "/" 
          return true
        end
        public_starts = [ "/Home", "/wiki", "/favicon.ico"] 
        LOGGER.debug(path)
        return true if public_starts.any? {|link| path.start_with?(link)}
        public_patterns = [ %r{\A/gollum/(assets|commit|history|last_commit_info).*}, %r{\A/gollum/search}, %r{\A/gollum/latest_changes\z}]
        public_patterns.any? {|pattern| pattern.match(path)}
    end
    def auth_path?(path)
      auth_paths = [%r{\A/gollum/(edit|create|rename)/.*\z}, %r{\A/gollum/overview}]  
      auth_paths.any? {|pattern| pattern.match(path)}
    end
    def authenticate!
      path = self.env['PATH_INFO']
      if public_path?(path) 
        return
      end
      @auth =  Rack::Auth::Basic::Request.new(request.env)
      unless auth_path?(path)
        throw(:halt, [403, 'Forbidden - You can not access anything outside wiki/ path.'])
      end
      if @auth.provided? && @auth.basic? && @auth.credentials && (@user = get_user(@auth.credentials))
        return 
      else
        response['WWW-Authenticate'] = %(Basic realm="Gollum Wiki")
        throw(:halt, [401, "Not authorized\n"])
      end
    end

    def authorize_write
      throw(:halt, [403, "Forbidden\n"]) unless @user.can_write
    end

    def users # User caching helper.
      @_users ||= settings.authorized_users.map {|u| User.new(*u) } # The ||= evalutes RHS only if left hand side is falsy.
    end

    def get_user(credentials)
      email = credentials[0]
      pass_hash = Digest::SHA1.hexdigest(credentials[1])
      users.find do |u| 
        [u.email, u.password_hash] ==
        [email, pass_hash]
      end
    end
  end

end
